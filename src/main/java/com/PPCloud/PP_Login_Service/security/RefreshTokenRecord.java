package com.PPCloud.PP_Login_Service.security;

public record RefreshTokenRecord(
        String tenantId,
        String userId,
        String clientId,
        String deviceHash,
        String tokenFamily,          // rotation 家族
        long issuedAtEpochSec,
        long expiresAtEpochSec,
        boolean revoked              // 业务撤销（非并发判断）
) {}