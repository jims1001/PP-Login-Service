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

/**
 * RedisTokenStore (industrial-ready)
 *
 * Key design:
 * 1) rtKey:   pp:idp:{tenant}:rt:{rtHash} -> HASH(MapCache) with TTL
 *    fields:
 *      - json (RefreshTokenRecord JSON)
 *      - status (ACTIVE/USED/REVOKED)
 *      - issuedAtMs
 *      - usedAtMs
 *      - rotatedTo (new rtHash)
 *      - userId, clientId, deviceHash
 *
 * 2) rtIndexKey: pp:idp:{tenant}:rtidx:v2:{userId}:{clientId}:{deviceHash} -> ZSET(rtHash), score=issuedAtMs, with TTL
 *
 * 3) access blacklist: pp:idp:{tenant}:bl:{jti} -> "1" with TTL
 */
@Component
public class RedisTokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStore.class);

    private final RedissonClient redisson;
    private final ObjectMapper om;

    // 扫描用户索引用（避免 KEYS）
    private static final int SCAN_COUNT = 1000;

    // rt hash fields
    private static final String F_JSON = "json";
    private static final String F_STATUS = "status";
    private static final String F_ISSUED_AT = "issuedAtMs";
    private static final String F_USED_AT = "usedAtMs";
    private static final String F_ROTATED_TO = "rotatedTo";
    private static final String F_USER_ID = "userId";
    private static final String F_CLIENT_ID = "clientId";
    private static final String F_DEVICE_HASH = "deviceHash";
    private static final String F_REVOKED_AT = "revokedAtMs";

    // statuses
    public static final String ST_ACTIVE = "ACTIVE";
    public static final String ST_USED = "USED";
    public static final String ST_REVOKED = "REVOKED";

    public RedisTokenStore(RedissonClient redisson, ObjectMapper om) {
        this.redisson = redisson;
        this.om = om;
    }


    // -------------------------
    // Key builders
    // -------------------------

    /** refresh record: rtHash -> HASH(MapCache) with TTL */
    public String rtKey(String tenantId, String rtHash) {
        return "pp:idp:" + tenantId + ":rt:" + rtHash;
    }

    /**
     * rt index: (userId, clientId, deviceHash) -> ZSET(rtHash)
     * score = issuedAtEpochMs，用于排序裁剪（最旧的先删）
     */
    public String rtIndexKey(String tenantId, String userId, String clientId, String deviceHash) {
        return "pp:idp:" + tenantId + ":rtidx:v2:" + userId + ":" + clientId + ":" + deviceHash;
    }

    /** per-device lock key name (Redisson manages lock internals, do NOT expire this key manually) */
    private String rtIndexLockKey(String tenantId, String userId, String clientId, String deviceHash) {
        return rtIndexKey(tenantId, userId, clientId, deviceHash) + ":lock";
    }

    public String accessBlacklistKey(String tenantId, String jti) {
        return "pp:idp:" + tenantId + ":bl:" + jti;
    }

    // --- DeviceSession (新增，不影响原方法名) ---
    public String deviceSessionKey(String tenantId, String userId, String clientId, String deviceHash) {
        return "pp:idp:" + tenantId + ":ds:v1:" + userId + ":" + clientId + ":" + deviceHash;
    }

    // -------------------------
    // WRONGTYPE 防护
    // -------------------------

    private void ensureTypeOrReset(String key, RType expected) {
        RType type = redisson.getKeys().getType(key);
        if (type == null) return; // 不存在

        if (type != expected) {
            String backup = key + ":badtype:" + type + ":" + System.currentTimeMillis();
            try {
                redisson.getKeys().rename(key, backup);
                log.warn("Redis key WRONGTYPE detected. rename {} -> {}", key, backup);
            } catch (Exception renameEx) {
                log.warn("Redis key WRONGTYPE detected. rename failed, will delete. key={}, type={}", key, type, renameEx);
                redisson.getKeys().delete(key);
            }
        }
    }

    /**
     * Hash-like 防护：不同 Redisson 版本 getType() 可能返回 MAP/HASH
     * 这里做兼容，避免误伤正常 key。
     */
    private void ensureHashLikeOrReset(String key) {
        RType type = redisson.getKeys().getType(key);
        if (type == null) return;

        // 你这个版本没有 RType.HASH，所以不能写 type == RType.HASH。
        // 采取最稳策略：只在“明显冲突类型”时才 reset。
        boolean definitelyNotHashLike =
                type == RType.ZSET ||
                        type == RType.SET  ||
                        type == RType.LIST ;

        // 其它类型（含 MAP / 以及未来可能出现的 HASH/STREAM/…）一律认为“可能是 hash-like”，不要动。
        if (!definitelyNotHashLike) return;

        String backup = key + ":badtype:" + type + ":" + System.currentTimeMillis();
        try {
            redisson.getKeys().rename(key, backup);
            log.warn("Redis key WRONGTYPE detected (hash-like expected). rename {} -> {}", key, backup);
        } catch (Exception renameEx) {
            log.warn("Redis key WRONGTYPE detected (hash-like expected). rename failed, will delete. key={}, type={}",
                    key, type, renameEx);
            redisson.getKeys().delete(key);
        }
    }

    // -------------------------
    // Refresh token record (rt:* -> HASH with TTL)
    // -------------------------

    private RMapCache<String, String> rtMap(String tenantId, String rtHash) {
        String key = rtKey(tenantId, rtHash);
        ensureHashLikeOrReset(key);
        return redisson.getMapCache(key, StringCodec.INSTANCE);
    }

    public void putRefreshRecord(String tenantId, String rtHash, RefreshTokenRecord rec, Duration ttl) {
        if (rec == null) throw new IllegalArgumentException("rec is null");
        if (ttl == null || ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("ttl must be > 0");

        // tenant 一致性校验（你强调必须保留 tenantId）
        if (rec.tenantId() != null && !tenantId.equals(rec.tenantId())) {
            throw new IllegalArgumentException("REFRESH_TENANT_MISMATCH_RECORD");
        }

        try {
            String json = om.writeValueAsString(rec);
            RMapCache<String, String> m = rtMap(tenantId, rtHash);

            Map<String, String> fields = new HashMap<>();
            fields.put(F_JSON, json);
            fields.put(F_STATUS, ST_ACTIVE);

            long issuedAtMs = rec.issuedAtEpochSec() > 0 ? rec.issuedAtEpochSec() * 1000L : System.currentTimeMillis();
            fields.put(F_ISSUED_AT, String.valueOf(issuedAtMs));

            // 冗余字段：加速 revoke / family 推导
            if (rec.userId() != null) fields.put(F_USER_ID, rec.userId());
            if (rec.clientId() != null) fields.put(F_CLIENT_ID, rec.clientId());
            if (rec.deviceHash() != null) fields.put(F_DEVICE_HASH, rec.deviceHash());


            m.putAll(fields);
            redisson.getKeys().expire(
                    rtKey(tenantId, rtHash),
                    ttl.toMillis(),
                    TimeUnit.MILLISECONDS
            );

        } catch (Exception e) {
            throw new RuntimeException("REDIS_PUT_REFRESH_FAILED", e);
        }
    }

    public RefreshTokenRecord getRefreshRecord(String tenantId, String rtHash) {
        try {
            RMapCache<String, String> m = rtMap(tenantId, rtHash);
            String json = m.get(F_JSON);
            if (json == null) return null;
            return om.readValue(json, RefreshTokenRecord.class);
        } catch (Exception e) {
            throw new RuntimeException("REDIS_GET_REFRESH_FAILED", e);
        }
    }

    public String getRefreshStatus(String tenantId, String rtHash) {
        RMapCache<String, String> m = rtMap(tenantId, rtHash);
        return m.get(F_STATUS);
    }

    public boolean isRefreshActive(String tenantId, String rtHash) {
        String s = getRefreshStatus(tenantId, rtHash);
        return ST_ACTIVE.equals(s);
    }

    public void deleteRefreshRecord(String tenantId, String rtHash) {
        redisson.getKeys().delete(rtKey(tenantId, rtHash));
    }

    // -------------------------
    // Index (rtidx:* -> ZSET<rtHash>)
    // -------------------------

    private RScoredSortedSet<String> rtIndexZset(String idxKey) {
        ensureTypeOrReset(idxKey, RType.ZSET);
        return redisson.getScoredSortedSet(idxKey, StringCodec.INSTANCE);
    }

    public void addToIndexAndTrim(String tenantId,
                                  String userId,
                                  String clientId,
                                  String deviceHash,
                                  String rtHash,
                                  int maxPerDevice,
                                  Duration ttl) {

        if (maxPerDevice <= 0) throw new IllegalArgumentException("maxPerDevice must be > 0");
        if (ttl == null || ttl.isNegative() || ttl.isZero())
            throw new IllegalArgumentException("ttl must be > 0");

        String idxKey = rtIndexKey(tenantId, userId, clientId, deviceHash);
        String lockKey = rtIndexLockKey(tenantId, userId, clientId, deviceHash);

        RLock lock = redisson.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(2, 5, TimeUnit.SECONDS);

            long now = System.currentTimeMillis();
            RScoredSortedSet<String> zset = rtIndexZset(idxKey);

            zset.add(now, rtHash);
            zset.expire(ttl);

            if (!locked) return;

            int size = zset.size();
            int over = size - maxPerDevice;
            if (over <= 0) return;

            Collection<String> victims = zset.valueRange(0, over - 1);
            if (victims == null || victims.isEmpty()) return;

            zset.removeAll(victims);

            // 标记 REVOKED（仅当 victim rtKey 仍存在，避免创建新 hash）
            RBatch batch = redisson.createBatch(BatchOptions.defaults());
            Map<String, RFuture<Boolean>> exists = new HashMap<>(victims.size());
            for (String victimHash : victims) {
                exists.put(victimHash,
                        batch.getBucket(rtKey(tenantId, victimHash), StringCodec.INSTANCE).isExistsAsync());
            }
            batch.execute();

            RBatch batch2 = redisson.createBatch(BatchOptions.defaults());
            for (String victimHash : victims) {
                Boolean ok = exists.get(victimHash).toCompletableFuture().getNow(false);
                if (!Boolean.TRUE.equals(ok)) continue;

                RMapCacheAsync<String, String> m =
                        batch2.getMapCache(rtKey(tenantId, victimHash), StringCodec.INSTANCE);

                m.putAsync(F_STATUS, ST_REVOKED);
                m.putAsync(F_REVOKED_AT, String.valueOf(now));

// ✅ 用 RKeys 统一管理 TTL（无 deprecated，兼容你当前 Redisson）
                batch2.getKeys().expireAsync(
                        rtKey(tenantId, victimHash),
                        ttl.toMillis(),
                        TimeUnit.MILLISECONDS
                );
            }
            batch2.execute();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked) {
                try { lock.unlock(); } catch (Exception ignore) {}
            }
        }
    }

    public List<String> getIndexMembersNewestFirst(String tenantId, String userId, String clientId, String deviceHash) {
        String idxKey = rtIndexKey(tenantId, userId, clientId, deviceHash);
        RScoredSortedSet<String> zset = rtIndexZset(idxKey);

        Collection<String> all = zset.valueRangeReversed(0, -1);
        if (all == null || all.isEmpty()) return Collections.emptyList();

        List<String> out = new ArrayList<>(all.size());
        List<String> stale = new ArrayList<>();

        // ✅ 兼容性最好：batch + bucket.isExistsAsync()
        RBatch batch = redisson.createBatch(BatchOptions.defaults());
        Map<String, RFuture<Boolean>> existsFutures = new HashMap<>(all.size());
        for (String h : all) {
            existsFutures.put(h, batch.getBucket(rtKey(tenantId, h), StringCodec.INSTANCE).isExistsAsync());
        }
        batch.execute();

        for (String h : all) {
            Boolean exists = existsFutures.get(h).toCompletableFuture().getNow(false);
            if (Boolean.TRUE.equals(exists)) out.add(h);
            else stale.add(h);
        }

        if (!stale.isEmpty()) zset.removeAll(stale);
        return out;
    }

    public void removeFromIndex(String tenantId, String userId, String clientId, String deviceHash, String rtHash) {
        String idxKey = rtIndexKey(tenantId, userId, clientId, deviceHash);
        rtIndexZset(idxKey).remove(rtHash);
    }

    public Set<String> scanUserIndexes(String tenantId, String userId) {
        String pattern = "pp:idp:" + tenantId + ":rtidx:v2:" + userId + ":*";
        Set<String> out = new HashSet<>();
        Iterable<String> it = redisson.getKeys().getKeysByPattern(pattern, SCAN_COUNT);
        for (String k : it) out.add(k);
        return out;
    }

    public int revokeAllForDevice(String tenantId, String userId, String clientId, String deviceHash) {
        String idxKey = rtIndexKey(tenantId, userId, clientId, deviceHash);
        RScoredSortedSet<String> zset = rtIndexZset(idxKey);

        Collection<String> hashes = zset.readAll();
        if (hashes == null || hashes.isEmpty()) {
            redisson.getKeys().delete(idxKey);
            return 0;
        }

        RBatch batch = redisson.createBatch(BatchOptions.defaults());
        for (String h : hashes) {
            batch.getKeys().deleteAsync(rtKey(tenantId, h));
        }
        batch.getKeys().deleteAsync(idxKey);
        batch.execute();

        return hashes.size();
    }

    // -------------------------
    // Access token blacklist
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

    public Set<String> getIndexMembers(String idxKey) {
        RScoredSortedSet<String> zset = redisson.getScoredSortedSet(idxKey, StringCodec.INSTANCE);
        Collection<String> all = zset.readAll();
        if (all == null || all.isEmpty()) return Collections.emptySet();
        return new LinkedHashSet<>(all);
    }

    public void deleteKey(String key) {
        redisson.getKeys().delete(key);
    }

    // =========================================================
    // =============== 新增：工业级扩展方法 ======================
    // =========================================================

    public boolean markRefreshUsedOnce(String tenantId,
                                       String rtHash,
                                       String rotatedToRtHash,
                                       Duration ttlForAudit) {

        if (ttlForAudit == null || ttlForAudit.isNegative() || ttlForAudit.isZero()) {
            throw new IllegalArgumentException("ttlForAudit must be > 0");
        }

        String key = rtKey(tenantId, rtHash);
        ensureHashLikeOrReset(key);

        long now = System.currentTimeMillis();

        String lua =
                "local s = redis.call('HGET', KEYS[1], ARGV[1]) " +
                        "if (not s) then return 0 end " +
                        "if (s ~= ARGV[2]) then return 0 end " +
                        "redis.call('HSET', KEYS[1], ARGV[1], ARGV[3]) " +
                        "redis.call('HSET', KEYS[1], ARGV[4], ARGV[5]) " +
                        "if (ARGV[6] ~= '') then redis.call('HSET', KEYS[1], ARGV[7], ARGV[6]) end " +
                        "return 1";

        Number r = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                lua,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(key),
                F_STATUS,
                ST_ACTIVE,
                ST_USED,
                F_USED_AT,
                String.valueOf(now),
                rotatedToRtHash == null ? "" : rotatedToRtHash,
                F_ROTATED_TO
        );

        boolean ok = r != null && r.longValue() == 1L;
        if (!ok) return false;

        try {
            redisson.getKeys().expire(key, ttlForAudit.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ignore) {
        }

        return true;
    }

    public int revokeFamilyByRefreshHash(String tenantId, String rtHash) {
        RMapCache<String, String> m = rtMap(tenantId, rtHash);

        String userId = m.get(F_USER_ID);
        String clientId = m.get(F_CLIENT_ID);
        String deviceHash = m.get(F_DEVICE_HASH);

        // ✅ 历史数据兜底：冗余字段缺失就从 json 解析
        if (userId == null || clientId == null || deviceHash == null) {
            String json = m.get(F_JSON);
            if (json != null) {
                try {
                    RefreshTokenRecord rec = om.readValue(json, RefreshTokenRecord.class);
                    if (userId == null) userId = rec.userId();
                    if (clientId == null) clientId = rec.clientId();
                    if (deviceHash == null) deviceHash = rec.deviceHash();
                } catch (Exception ignore) {
                }
            }
        }

        if (userId == null || clientId == null || deviceHash == null) return 0;
        return revokeAllForDevice(tenantId, userId, clientId, deviceHash);
    }

    public int revokeAllForUser(String tenantId, String userId) {
        Set<String> idxKeys = scanUserIndexes(tenantId, userId);
        if (idxKeys.isEmpty()) return 0;

        int total = 0;
        for (String idxKey : idxKeys) {
            String[] parts = idxKey.split(":");
            if (parts.length < 8) continue;
            String clientId = parts[6];
            String deviceHash = parts[7];
            total += revokeAllForDevice(tenantId, userId, clientId, deviceHash);
        }
        return total;
    }

    public int revokeAllForClient(String tenantId, String userId, String clientId) {
        String pattern = "pp:idp:" + tenantId + ":rtidx:v2:" + userId + ":" + clientId + ":*";
        Iterable<String> it = redisson.getKeys().getKeysByPattern(pattern, SCAN_COUNT);

        int total = 0;
        for (String idxKey : it) {
            String[] parts = idxKey.split(":");
            if (parts.length < 8) continue;
            String deviceHash = parts[7];
            total += revokeAllForDevice(tenantId, userId, clientId, deviceHash);
        }
        return total;
    }

    public int cleanupUserIndexes(String tenantId, String userId) {
        Set<String> idxKeys = scanUserIndexes(tenantId, userId);
        if (idxKeys.isEmpty()) return 0;

        int removed = 0;
        for (String idxKey : idxKeys) {
            RScoredSortedSet<String> zset = rtIndexZset(idxKey);
            Collection<String> members = zset.readAll();
            if (members == null || members.isEmpty()) {
                redisson.getKeys().delete(idxKey);
                continue;
            }

            // ✅ batch exists，避免 N 次同步 IO
            RBatch batch = redisson.createBatch(BatchOptions.defaults());
            Map<String, RFuture<Boolean>> ex = new HashMap<>(members.size());
            for (String h : members) {
                ex.put(h, batch.getBucket(rtKey(tenantId, h), StringCodec.INSTANCE).isExistsAsync());
            }
            batch.execute();

            List<String> stale = new ArrayList<>();
            for (String h : members) {
                Boolean exists = ex.get(h).toCompletableFuture().getNow(false);
                if (!Boolean.TRUE.equals(exists)) stale.add(h);
            }

            if (!stale.isEmpty()) {
                zset.removeAll(stale);
                removed += stale.size();
            }

            if (zset.isEmpty()) redisson.getKeys().delete(idxKey);
        }
        return removed;
    }

    // -------------------------
    // DeviceSession（新增）
    // -------------------------

    public void upsertDeviceSession(String tenantId,
                                    String userId,
                                    String clientId,
                                    String deviceHash,
                                    Map<String, String> attrs,
                                    Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("ttl must be > 0");

        String key = deviceSessionKey(tenantId, userId, clientId, deviceHash);
        ensureHashLikeOrReset(key);

        RMapCache<String, String> m = redisson.getMapCache(key, StringCodec.INSTANCE);

        Map<String, String> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("clientId", clientId);
        payload.put("deviceHash", deviceHash);
        payload.put("lastSeenMs", String.valueOf(System.currentTimeMillis()));
        if (attrs != null) payload.putAll(attrs);

        m.putAll(payload);
        redisson.getKeys().expire(
                m.getName(),
                ttl.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    public Map<String, String> getDeviceSession(String tenantId, String userId, String clientId, String deviceHash) {
        String key = deviceSessionKey(tenantId, userId, clientId, deviceHash);
        ensureHashLikeOrReset(key);

        RMapCache<String, String> m = redisson.getMapCache(key, StringCodec.INSTANCE);
        Map<String, String> all = m.readAllMap();
        return all == null ? Collections.emptyMap() : all;
    }

    public void deleteDeviceSession(String tenantId, String userId, String clientId, String deviceHash) {
        redisson.getKeys().delete(deviceSessionKey(tenantId, userId, clientId, deviceHash));
    }

    public Set<String> scanUserDeviceSessions(String tenantId, String userId) {
        String pattern = "pp:idp:" + tenantId + ":ds:v1:" + userId + ":*";
        Set<String> out = new HashSet<>();
        Iterable<String> it = redisson.getKeys().getKeysByPattern(pattern, SCAN_COUNT);
        for (String k : it) out.add(k);
        return out;
    }
}
