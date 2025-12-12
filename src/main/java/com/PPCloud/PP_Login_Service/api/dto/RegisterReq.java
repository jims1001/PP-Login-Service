package com.PPCloud.PP_Login_Service.api.dto;


public record RegisterReq(
        String tenantId,
        String clientId,
        String ip,
        String ua,
        String deviceFingerprint,

        String identifierType, // EMAIL|PHONE|USERNAME
        String identifier,
        String displayName,
        String password
) {}