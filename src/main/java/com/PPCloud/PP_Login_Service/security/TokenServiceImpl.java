package com.PPCloud.PP_Login_Service.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class TokenServiceImpl implements TokenService {

    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final TokenKeyProvider keys;
    private final RedisTokenStore store;

    // 可选：开启 access 黑名单（强制注销立刻生效才需要）
    private final boolean enableAccessBlacklist = false;

    public TokenServiceImpl(TokenKeyProvider keys, RedisTokenStore store) {
        this.keys = keys;
        this.store = store;
    }


    @Override
    public TokenPair issueLoginTokens(TokenIssueContext ctx) {
        Objects.requireNonNull(ctx.tenantId());
        Objects.requireNonNull(ctx.clientId());
        Objects.requireNonNull(ctx.userId());

        Instant now = Instant.now();

        String jti = UUID.randomUUID().toString();
        String access = signAccessJwt(ctx, now, jti);

        String refreshPlain = TokenCrypto.newOpaqueToken(48);
        String rtHash = TokenCrypto.sha256Base64Url(refreshPlain);

        String deviceHash = TokenCrypto.deviceHash(ctx.deviceFingerprint());
        String family = UUID.randomUUID().toString();

        long exp = now.plusSeconds(ctx.refreshTtlSeconds()).getEpochSecond();

        RefreshTokenRecord rec = new RefreshTokenRecord(
                ctx.tenantId(),
                ctx.userId(),
                ctx.clientId(),
                deviceHash,
                family,
                now.getEpochSecond(),
                exp,
                false
        );

        Duration rtTtl = Duration.ofSeconds(ctx.refreshTtlSeconds());
        store.putRefreshRecord(ctx.tenantId(), rtHash, rec, rtTtl);
        store.addToIndexAndTrim(ctx.tenantId(), ctx.userId(), ctx.clientId(), deviceHash, rtHash, 2, rtTtl);

        return new TokenPair(access, "Bearer", ctx.accessTtlSeconds(), refreshPlain);
    }

    @Override
    public TokenPair refresh(TokenRefreshContext ctx) {
        Objects.requireNonNull(ctx.tenantId());
        Objects.requireNonNull(ctx.clientId());
        Objects.requireNonNull(ctx.refreshToken());

        String tenantId = ctx.tenantId();
        String clientId = ctx.clientId();

        String rtHash = TokenCrypto.sha256Base64Url(ctx.refreshToken());

        // 1) 读 record
        RefreshTokenRecord rec = store.getRefreshRecord(tenantId, rtHash);
        if (rec == null) {
            throw new IllegalArgumentException("REFRESH_INVALID");
        }

        // ✅ tenant 一致性校验（你 record 里有 tenantId，就应该校验）
        if (!tenantId.equals(rec.tenantId())) {
            throw new IllegalArgumentException("REFRESH_TENANT_MISMATCH");
        }

        // 2) client / device 校验
        if (!rec.clientId().equals(clientId)) {
            throw new IllegalArgumentException("REFRESH_CLIENT_MISMATCH");
        }

        String deviceHash = TokenCrypto.deviceHash(ctx.deviceFingerprint());
        if (!rec.deviceHash().equals(deviceHash)) {
            throw new IllegalArgumentException("REFRESH_DEVICE_MISMATCH");
        }

        // 3) 过期校验
        Instant now = Instant.now();
        if (now.getEpochSecond() >= rec.expiresAtEpochSec()) {
            store.deleteRefreshRecord(tenantId, rtHash);
            store.removeFromIndex(tenantId, rec.userId(), rec.clientId(), rec.deviceHash(), rtHash);
            throw new IllegalArgumentException("REFRESH_EXPIRED");
        }

        // 4) 业务撤销校验（可选，但你 record 有 revoked 就应该用）
        if (rec.revoked()) {
            throw new IllegalArgumentException("REFRESH_REVOKED");
        }

        // 5) 生成 new refresh
        int accessTtl = 3600;
        int refreshTtl = 30 * 24 * 3600;
        Duration rtTtl = Duration.ofSeconds(refreshTtl);

        String newRefreshPlain = TokenCrypto.newOpaqueToken(48);
        String newRtHash = TokenCrypto.sha256Base64Url(newRefreshPlain);

        // 6) 原子消费旧 refresh：ACTIVE -> USED
        boolean ok = store.markRefreshUsedOnce(tenantId, rtHash, newRtHash, rtTtl);
        if (!ok) {
            store.revokeFamilyByRefreshHash(tenantId, rtHash);
            throw new IllegalArgumentException("REFRESH_REUSE_DETECTED");
        }

        // 7) rotation：旧的从索引移除
        store.removeFromIndex(tenantId, rec.userId(), rec.clientId(), rec.deviceHash(), rtHash);

        // 8) 签新 access（这里用 tenantId/clientId/userId）
        String jti = UUID.randomUUID().toString();
        String access = signAccessJwt(new TokenIssueContext(
                tenantId,
                clientId,
                rec.userId(),
                ctx.deviceFingerprint(),
                null,
                null,
                "refresh",
                accessTtl,
                refreshTtl
        ), now, jti);

        // 9) 构造新的 RefreshTokenRecord —— ✅ 注意：record 现在第一个参数是 tenantId
        long newExp = now.plusSeconds(refreshTtl).getEpochSecond();
        RefreshTokenRecord newRec = new RefreshTokenRecord(
                tenantId,                 // ✅ tenantId 必须补上，否则编译不过
                rec.userId(),
                clientId,
                deviceHash,               // 用本次计算后的 deviceHash（等价于 rec.deviceHash()）
                rec.tokenFamily(),
                now.getEpochSecond(),
                newExp,
                false
        );

        store.putRefreshRecord(tenantId, newRtHash, newRec, rtTtl);
        store.addToIndexAndTrim(tenantId, rec.userId(), clientId, deviceHash, newRtHash, 2, rtTtl);

        return new TokenPair(access, "Bearer", accessTtl, newRefreshPlain);
    }

    @Override
    public void revokeByRefreshToken(String tenantId, String refreshToken) {
        String rtHash = TokenCrypto.sha256Base64Url(refreshToken);
        RefreshTokenRecord rec = store.getRefreshRecord(tenantId, rtHash);
        if (rec != null) {
            store.deleteRefreshRecord(tenantId, rtHash);
            store.removeFromIndex(tenantId, rec.userId(), rec.clientId(), rec.deviceHash(), rtHash);
        }
    }

    @Override
    public void revokeDevice(String tenantId, String userId, String clientId, String deviceFingerprint) {
        String deviceHash = TokenCrypto.deviceHash(deviceFingerprint);
        List<String> hashes =  store.getIndexMembersNewestFirst(tenantId, userId, clientId, deviceHash);
        if (hashes == null) return;

        for (String rtHash : hashes) {
            store.deleteRefreshRecord(tenantId, rtHash);
            store.removeFromIndex(tenantId, userId, clientId, deviceHash, rtHash);
        }
    }

    @Override
    public void revokeAll(String tenantId, String userId) {
        Set<String> idxKeys = store.scanUserIndexes(tenantId, userId);
        if (idxKeys == null) return;

        for (String idxKey : idxKeys) {
            Set<String> hashes = store.getIndexMembers(idxKey);
            if (hashes == null) continue;

            // idxKey: pp:idp:{tenantId}:rtidx:{userId}:{clientId}:{deviceHash}
            String[] parts = idxKey.split(":");
            String clientId = parts[6];
            String deviceHash = parts[7];

            for (String rtHash : hashes) {
                store.deleteRefreshRecord(tenantId, rtHash);
                store.removeFromIndex(tenantId, userId, clientId, deviceHash, rtHash);
            }
            store.deleteKey(idxKey);
        }
    }

    @Override
    public AccessTokenClaims verifyAccessToken(String jwt) {
        try {
            SignedJWT sjwt = SignedJWT.parse(jwt);

            JWSVerifier verifier = new RSASSAVerifier(keys.signingKey().toRSAPublicKey());
            if (!sjwt.verify(verifier)) {
                throw new IllegalArgumentException("ACCESS_BAD_SIGNATURE");
            }

            JWTClaimsSet c = sjwt.getJWTClaimsSet();
            Instant exp = c.getExpirationTime().toInstant();
            Instant now = Instant.now();
            if (now.isAfter(exp)) {
                throw new IllegalArgumentException("ACCESS_EXPIRED");
            }

            String tenantId = (String) c.getClaim("tenantId");
            String jti = c.getJWTID();

            if (enableAccessBlacklist && store.isAccessJtiBlacklisted(tenantId, jti)) {
                throw new IllegalArgumentException("ACCESS_REVOKED");
            }

            return new AccessTokenClaims(
                    c.getIssuer(),
                    jti,
                    c.getSubject(),
                    tenantId,
                    (String) c.getClaim("clientId"),
                    c.getAudience(),
                    c.getIssueTime().toInstant(),
                    exp,
                    (String) c.getClaim("tokenType"),
                    (String) c.getClaim("amr")
            );
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalArgumentException("ACCESS_INVALID", e);
        }
    }

    // --- internal ---
    private String signAccessJwt(TokenIssueContext ctx, Instant now, String jti) {
        try {
            String issuer = "pp-idp"; // 以后你可以改为 instance/issuer

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(ctx.userId())
                    .audience(ctx.clientId())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(ctx.accessTtlSeconds())))
                    .jwtID(jti)
                    .claim("tenantId", ctx.tenantId())
                    .claim("clientId", ctx.clientId())
                    .claim("tokenType", TOKEN_TYPE_ACCESS)
                    .claim("amr", ctx.authMethod())
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .keyID(keys.signingKey().getKeyID())
                    .build();

            SignedJWT jwt = new SignedJWT(header, claims);
            JWSSigner signer = new RSASSASigner(keys.signingKey().toRSAPrivateKey());
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("ISSUE_ACCESS_FAILED", e);
        }
    }
}