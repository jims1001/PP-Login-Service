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
        store.addToIndex(ctx.tenantId(), ctx.userId(), ctx.clientId(), deviceHash, rtHash, rtTtl);

        return new TokenPair(access, "Bearer", ctx.accessTtlSeconds(), refreshPlain);
    }

    @Override
    public TokenPair refresh(TokenRefreshContext ctx) {
        Objects.requireNonNull(ctx.tenantId());
        Objects.requireNonNull(ctx.clientId());
        Objects.requireNonNull(ctx.refreshToken());

        String rtHash = TokenCrypto.sha256Base64Url(ctx.refreshToken());
        RefreshTokenRecord rec = store.getRefreshRecord(ctx.tenantId(), rtHash);
        if (rec == null) {
            throw new IllegalArgumentException("REFRESH_INVALID");
        }
        if (rec.revoked()) {
            throw new IllegalArgumentException("REFRESH_REVOKED");
        }
        if (!rec.clientId().equals(ctx.clientId())) {
            throw new IllegalArgumentException("REFRESH_CLIENT_MISMATCH");
        }

        String deviceHash = TokenCrypto.deviceHash(ctx.deviceFingerprint());
        if (!rec.deviceHash().equals(deviceHash)) {
            throw new IllegalArgumentException("REFRESH_DEVICE_MISMATCH");
        }

        Instant now = Instant.now();
        if (now.getEpochSecond() >= rec.expiresAtEpochSec()) {
            // 过期：清理
            store.deleteRefreshRecord(ctx.tenantId(), rtHash);
            store.removeFromIndex(ctx.tenantId(), rec.userId(), rec.clientId(), rec.deviceHash(), rtHash);
            throw new IllegalArgumentException("REFRESH_EXPIRED");
        }

        // --- Rotation：旧 refresh 用一次就废 ---
        store.deleteRefreshRecord(ctx.tenantId(), rtHash);
        store.removeFromIndex(ctx.tenantId(), rec.userId(), rec.clientId(), rec.deviceHash(), rtHash);

        // 发新 access + 新 refresh（沿用同一 family）
        int accessTtl = 3600;           // 你也可以从租户策略读取
        int refreshTtl = 30 * 24 * 3600;

        String jti = UUID.randomUUID().toString();
        String access = signAccessJwt(new TokenIssueContext(
                rec.tenantId(),
                rec.clientId(),
                rec.userId(),

                ctx.deviceFingerprint(),
                null,
                null,
                "refresh",
                accessTtl,
                refreshTtl
        ), now, jti);

        String newRefreshPlain = TokenCrypto.newOpaqueToken(48);
        String newRtHash = TokenCrypto.sha256Base64Url(newRefreshPlain);

        long newExp = now.plusSeconds(refreshTtl).getEpochSecond();
        RefreshTokenRecord newRec = new RefreshTokenRecord(
                rec.tenantId(),
                rec.userId(),
                rec.clientId(),
                rec.deviceHash(),
                rec.tokenFamily(),
                now.getEpochSecond(),
                newExp,
                false
        );

        Duration rtTtl = Duration.ofSeconds(refreshTtl);
        store.putRefreshRecord(rec.tenantId(), newRtHash, newRec, rtTtl);
        store.addToIndex(rec.tenantId(), rec.userId(), rec.clientId(), rec.deviceHash(), newRtHash, rtTtl);

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
        Set<String> hashes = store.getIndexMembers(tenantId, userId, clientId, deviceHash);
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
            Set<String> hashes = store.getSetMembers(idxKey);
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