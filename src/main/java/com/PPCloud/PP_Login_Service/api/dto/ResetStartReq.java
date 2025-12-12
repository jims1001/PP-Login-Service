package com.PPCloud.PP_Login_Service.api.dto;

public record ResetStartReq(
        String tenantId,
        String clientId,
        String ip,
        String ua,
        String deviceFingerprint,

        String identifierType,
        String identifier
) {}