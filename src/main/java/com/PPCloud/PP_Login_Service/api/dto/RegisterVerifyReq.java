package com.PPCloud.PP_Login_Service.api.dto;

public record RegisterVerifyReq(
        String tenantId,
        String clientId,
        String ip,
        String ua,
        String deviceFingerprint,

        String flowToken,
        String code
) {}