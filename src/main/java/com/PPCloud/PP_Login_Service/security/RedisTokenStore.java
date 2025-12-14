package com.PPCloud.PP_Login_Service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.*;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class RedisTokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStore.class);

    private final RedissonClient redisson;
    private final ObjectMapper om;

    // 可按需调大/调小：SCAN 每次拉取数量
    private static final int SCAN_COUNT = 1000;

    public RedisTokenStore(RedissonClient redisson, ObjectMapper om) {
        this.redisson = redisson;
        this.om = om;
    }

    // --- key builders ---
    public String rtKey(String tenantId, String rtHash) {
        return "pp:idp:" + tenantId + ":rt:" + rtHash;
    }

    /**
     * rt index for (userId, clientId, deviceHash) -> many rtHash
     *
     * 强烈建议上版本号：rtidx:v1:...，以后结构升级不会撞老数据
     */
    public String rtIndexKey(String tenantId, String userId, String clientId, String deviceHash) {
        return "pp:idp:" + tenantId + ":rtidx:v1:" + userId + ":" + clientId + ":" + deviceHash;
    }

    public String accessBlacklistKey(String tenantId, String jti) {
        return "pp:idp:" + tenantId + ":bl:" + jti;
    }

    // -------------------------
    // WRONGTYPE 防护：确保 rtidx 是 ZSET（RSetCache底层）
    // -------------------------

    private void ensureZsetOrReset(String key) {
        RType type = redisson.getKeys().getType(key);
        if (type == null) {
            // key 不存在，后续 RSetCache 会自动创建 ZSET
            return;
        }

        // RSetCache 必须是 ZSET
        if (type != RType.ZSET) {
            // 生产建议：rename 备份一下，方便追查是谁写坏的；不想备份就直接 delete
            String backup = key + ":badtype:" + type + ":" + System.currentTimeMillis();
            try {
                // 仅当 key 还在才 rename
                redisson.getKeys().rename(key, backup);
                log.warn("Redis key WRONGTYPE detected. rename {} -> {}", key, backup);
            } catch (Exception renameEx) {
                // rename 失败（可能并发删除/没权限/已不存在），直接尝试删
                log.warn("Redis key WRONGTYPE detected. rename failed, will delete. key={}, type={}", key, type, renameEx);
                redisson.getKeys().delete(key);
                return;
            }
            // 备份后，创建新结构时会自动生成（无需这里显式创建）
        }
    }

    private static long ttlMillis(Duration ttl) {
        // RSetCache.add 不能用 0；给最小 1ms
        long ms = (ttl == null) ? 0 : ttl.toMillis();
        return Math.max(ms, 1L);
    }

    // -------------------------
    // Refresh token record (rt:* -> JSON)
    // -------------------------

    public void putRefreshRecord(String tenantId, String rtHash, RefreshTokenRecord rec, Duration ttl) {
        if (rec == null) throw new IllegalArgumentException("rec is null");
        if (ttl == null || ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("ttl must be > 0");

        try {
            String key = rtKey(tenantId, rtHash);
            String json = om.writeValueAsString(rec);

            // 用 StringCodec 避免序列化开销/乱码风险
            RBucket<String> b = redisson.getBucket(key, StringCodec.INSTANCE);
            b.set(json, ttl);
        } catch (Exception e) {
            throw new RuntimeException("REDIS_PUT_REFRESH_FAILED", e);
        }
    }

    public RefreshTokenRecord getRefreshRecord(String tenantId, String rtHash) {
        try {
            String key = rtKey(tenantId, rtHash);
            RBucket<String> b = redisson.getBucket(key, StringCodec.INSTANCE);
            String v = b.get();
            if (v == null) return null;

            return om.readValue(v, RefreshTokenRecord.class);
        } catch (Exception e) {
            throw new RuntimeException("REDIS_GET_REFRESH_FAILED", e);
        }
    }

    public void deleteRefreshRecord(String tenantId, String rtHash) {
        redisson.getBucket(rtKey(tenantId, rtHash), StringCodec.INSTANCE).delete();
    }

    // -------------------------
    // Index (rtidx:* -> RSetCache<rtHash> with per-entry TTL)
    // -------------------------

    public void addToIndex(String tenantId,
                           String userId,
                           String clientId,
                           String deviceHash,
                           String rtHash,
                           Duration ttl) {

        String key = rtIndexKey(tenantId, userId, clientId, deviceHash);

        // ✅ 防止历史脏类型导致 WRONGTYPE
        ensureZsetOrReset(key);

        RSetCache<String> set = redisson.getSetCache(key, StringCodec.INSTANCE);
        set.add(rtHash, ttlMillis(ttl), TimeUnit.MILLISECONDS);
    }

    public Set<String> getIndexMembers(String tenantId, String userId, String clientId, String deviceHash) {
        String key = rtIndexKey(tenantId, userId, clientId, deviceHash);

        ensureZsetOrReset(key);

        RSetCache<String> set = redisson.getSetCache(key, StringCodec.INSTANCE);
        Set<String> members = set.readAll();
        return (members != null) ? members : Collections.emptySet();
    }

    public void removeFromIndex(String tenantId, String userId, String clientId, String deviceHash, String rtHash) {
        String key = rtIndexKey(tenantId, userId, clientId, deviceHash);

        ensureZsetOrReset(key);

        redisson.getSetCache(key, StringCodec.INSTANCE).remove(rtHash);
    }

    /**
     * 扫描用户的所有 rtidx key（避免 KEYS，使用 SCAN stream）
     */
    public Set<String> scanUserIndexes(String tenantId, String userId) {
        String pattern = "pp:idp:" + tenantId + ":rtidx:v1:" + userId + ":*";

        Set<String> out = new HashSet<>();
        Iterable<String> it = redisson.getKeys().getKeysByPattern(pattern, SCAN_COUNT);
        for (String k : it) out.add(k);
        return out;
    }

    // -------------------------
    // Access token blacklist (bl:* -> "1")
    // -------------------------

    public void blacklistAccessJti(String tenantId, String jti, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("ttl must be > 0");
        String key = accessBlacklistKey(tenantId, jti);
        redisson.getBucket(key, StringCodec.INSTANCE).set("1", ttl);
    }

    public boolean isAccessJtiBlacklisted(String tenantId, String jti) {
        String key = accessBlacklistKey(tenantId, jti);
        return redisson.getBucket(key, StringCodec.INSTANCE).isExists();
    }

    // -------------------------
    // 高性能：批量撤销某个 device 的所有 refresh token
    // （删除 rt:* 记录 + 清空 rtidx:*）
    // -------------------------

    /**
     * 一次性撤销该 (userId, clientId, deviceHash) 下所有 refresh token：
     * - 读出 rtHash 列表
     * - batch 删除 rt:* bucket
     * - 删除 rtidx key
     *
     * 适合：换设备 / 踢下线 / 强制重新登录
     */
    public int revokeAllForDevice(String tenantId, String userId, String clientId, String deviceHash) {
        String idxKey = rtIndexKey(tenantId, userId, clientId, deviceHash);

        ensureZsetOrReset(idxKey);

        RSetCache<String> set = redisson.getSetCache(idxKey, StringCodec.INSTANCE);
        Set<String> hashes = set.readAll();
        if (hashes == null || hashes.isEmpty()) {
            // idxKey 可能空了也可以删掉，减少垃圾
            redisson.getKeys().delete(idxKey);
            return 0;
        }

        RBatch batch = redisson.createBatch(BatchOptions.defaults());
        for (String rtHash : hashes) {
            String rtKey = rtKey(tenantId, rtHash);
            batch.getBucket(rtKey, StringCodec.INSTANCE).deleteAsync();
        }
        batch.getKeys().deleteAsync(idxKey);
        batch.execute();

        return hashes.size();
    }

    // -------------------------
    // 小工具：安全删除任意 key
    // -------------------------

    public void deleteKey(String key) {
        redisson.getKeys().delete(key);
    }


    public Set<Object> getSetMembers(String key) {
        return redisson.getSetCache(key).readAll();
    }

}