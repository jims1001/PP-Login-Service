package com.PPCloud.PP_Login_Service.security;

public record RefreshTokenRecord(
        String tenantId,
        String userId,
        String clientId,
        String deviceHash,
        String tokenFamily,    // 用于 rotation（可选但强烈建议）
        long issuedAtEpochSec,
        long expiresAtEpochSec,
        boolean revoked
) {}