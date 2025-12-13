package com.PPCloud.PP_Login_Service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
public class RedisTokenStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    public RedisTokenStore(StringRedisTemplate redis, ObjectMapper om) {
        this.redis = redis;
        this.om = om;
    }

    // --- key builders ---
    public String rtKey(String tenantId, String rtHash) {
        return "pp:idp:" + tenantId + ":rt:" + rtHash;
    }

    public String rtIndexKey(String tenantId, String userId, String clientId, String deviceHash) {
        return "pp:idp:" + tenantId + ":rtidx:" + userId + ":" + clientId + ":" + deviceHash;
    }

    public String accessBlacklistKey(String tenantId, String jti) {
        return "pp:idp:" + tenantId + ":bl:" + jti;
    }

    // --- refresh token record ---
    public void putRefreshRecord(String tenantId, String rtHash, RefreshTokenRecord rec, Duration ttl) {
        try {
            redis.opsForValue().set(rtKey(tenantId, rtHash), om.writeValueAsString(rec), ttl);
        } catch (Exception e) {
            throw new RuntimeException("REDIS_PUT_REFRESH_FAILED", e);
        }
    }

    public RefreshTokenRecord getRefreshRecord(String tenantId, String rtHash) {
        try {
            String v = redis.opsForValue().get(rtKey(tenantId, rtHash));
            if (v == null) return null;
            return om.readValue(v, RefreshTokenRecord.class);
        } catch (Exception e) {
            throw new RuntimeException("REDIS_GET_REFRESH_FAILED", e);
        }
    }

    public void deleteRefreshRecord(String tenantId, String rtHash) {
        redis.delete(rtKey(tenantId, rtHash));
    }

    // --- index set for device/user ---
    public void addToIndex(String tenantId, String userId, String clientId, String deviceHash, String rtHash, Duration ttl) {
        String key = rtIndexKey(tenantId, userId, clientId, deviceHash);
        redis.opsForSet().add(key, rtHash);
        redis.expire(key, ttl);
    }

    public Set<String> getIndexMembers(String tenantId, String userId, String clientId, String deviceHash) {
        return redis.opsForSet().members(rtIndexKey(tenantId, userId, clientId, deviceHash));
    }

    public void removeFromIndex(String tenantId, String userId, String clientId, String deviceHash, String rtHash) {
        redis.opsForSet().remove(rtIndexKey(tenantId, userId, clientId, deviceHash), rtHash);
    }

    public Set<String> scanUserIndexes(String tenantId, String userId) {
        // 简化版：用 keys（生产建议改成 scan）
        return redis.keys("pp:idp:" + tenantId + ":rtidx:" + userId + ":*");
    }

    // --- access token blacklist (optional) ---
    public void blacklistAccessJti(String tenantId, String jti, Duration ttl) {
        redis.opsForValue().set(accessBlacklistKey(tenantId, jti), "1", ttl);
    }

    public boolean isAccessJtiBlacklisted(String tenantId, String jti) {
        return Boolean.TRUE.equals(redis.hasKey(accessBlacklistKey(tenantId, jti)));
    }

    public Set<String> getSetMembers(String key) {
        return redis.opsForSet().members(key);
    }

    public void deleteKey(String key) {
        redis.delete(key);
    }

}