package com.PPCloud.PP_Login_Service.core.workflow;

import com.PPCloud.PP_Login_Service.port.IdentifierNormalizer;
import com.PPCloud.PP_Login_Service.port.PasswordHasher;
import com.PPCloud.PP_Login_Service.port.UserDataAccess;
import com.PPCloud.PP_Login_Service.security.TokenService;

/**
 * WorkflowContext：节点执行时需要的所有上下文
 *
 * - tenantId/clientId/ip/ua/deviceFingerprint/now：请求维度信息
 * - userDao：访问你的 8 张表
 * - audit：写 IamAuthAudit
 * - normalizer：identifier 规范化
 * - passwordHasher：密码 hash 校验
 */
public class WorkflowContext {

    public final String tenantId;
    public final String clientId;
    public final String ip;
    public final String ua;
    public final String deviceFingerprint;
    public final long now;

    public final UserDataAccess userDao;
    public final AuditSink audit;
    public final IdentifierNormalizer normalizer;
    public final PasswordHasher passwordHasher;
    public final TokenService tokenService;

    public WorkflowContext(
            String tenantId,
            String clientId,
            String ip,
            String ua,
            String deviceFingerprint,
            long now,
            UserDataAccess userDao,
            AuditSink audit,
            IdentifierNormalizer normalizer,
            PasswordHasher passwordHasher, TokenService tokenService
    ) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.ip = ip;
        this.ua = ua;
        this.deviceFingerprint = deviceFingerprint;
        this.now = now;
        this.userDao = userDao;
        this.audit = audit;
        this.normalizer = normalizer;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }
}