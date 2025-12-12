package com.PPCloud.PP_Login_Service.api.dto;

/**
 * ⚠️ 注意：这里 userId 只是为了让骨架先跑通
 * 生产建议：userId 从 actionToken payload 得出，不允许客户端传
 */
public record ResetCommitReq(
        String tenantId,
        String clientId,
        String ip,
        String ua,
        String deviceFingerprint,

        String flowToken,
        String resetToken,
        String userId,
        String newPassword
) {}