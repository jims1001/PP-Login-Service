package com.PPCloud.PP_Login_Service.api.dto;

public record LoginIdentifyReq(
        String tenantId,
        String clientId,
        String ip,
        String ua,
        String deviceFingerprint,

        String identifierType,
        String identifier
) {}
