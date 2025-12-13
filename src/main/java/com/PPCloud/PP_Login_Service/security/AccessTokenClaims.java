package com.PPCloud.PP_Login_Service.security;

import java.time.Instant;
import java.util.List;

public record AccessTokenClaims(
        String issuer,
        String jti,
        String subject,
        String tenantId,
        String clientId,
        List<String> audience,
        Instant issuedAt,
        Instant expiresAt,
        String tokenType,
        String authMethod
) {}