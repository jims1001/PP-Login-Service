package com.PPCloud.PP_Login_Service.security;

public record TokenIssueContext(
        String tenantId,
        String clientId,
        String userId,
        String deviceFingerprint,
        String ip,
        String ua,
        String authMethod,   // "pwd" / "otp" / "mfa"
        int accessTtlSeconds,
        int refreshTtlSeconds
) {}