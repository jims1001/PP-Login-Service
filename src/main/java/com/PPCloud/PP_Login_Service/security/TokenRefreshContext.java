package com.PPCloud.PP_Login_Service.security;


public record TokenRefreshContext(
        String tenantId,
        String clientId,
        String deviceFingerprint,
        String refreshToken
) {}