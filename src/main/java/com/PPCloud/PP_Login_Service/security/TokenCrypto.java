package com.PPCloud.PP_Login_Service.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class TokenCrypto {
    private static final SecureRandom RNG = new SecureRandom();

    private TokenCrypto() {}

    /** 生成 refreshToken 明文（返回给客户端） */
    public static String newOpaqueToken(int bytes) {
        byte[] b = new byte[bytes];
        RNG.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    /** 用于 Redis key：不要存明文 refreshToken */
    public static String sha256Base64Url(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** deviceFingerprint 也建议 hash 后用于 key/比较 */
    public static String deviceHash(String deviceFingerprint) {
        if (deviceFingerprint == null) return "no_device";
        return sha256Base64Url(deviceFingerprint);
    }
}