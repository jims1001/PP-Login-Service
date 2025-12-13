package com.PPCloud.PP_Login_Service.security;

public interface TokenService {

    TokenPair issueLoginTokens(TokenIssueContext ctx);

    TokenPair refresh(TokenRefreshContext ctx);

    void revokeByRefreshToken(String tenantId, String refreshToken);     // 单点注销
    void revokeDevice(String tenantId, String userId, String clientId, String deviceFingerprint); // 踢设备
    void revokeAll(String tenantId, String userId);                      // 踢全端

    AccessTokenClaims verifyAccessToken(String jwt); // 只做 JWT 验签+黑名单检查(可选)
}