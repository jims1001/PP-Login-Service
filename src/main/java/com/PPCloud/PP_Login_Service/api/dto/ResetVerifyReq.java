package com.PPCloud.PP_Login_Service.api.dto;

public record ResetVerifyReq(
        String tenantId,
        String clientId,
        String ip,
        String ua,
        String deviceFingerprint,

        String flowToken,
        String identifierType,
        String identifier,
        String code
) {}