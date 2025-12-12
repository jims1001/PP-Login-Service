package com.PPCloud.PP_Login_Service.flow;

import com.PPCloud.PP_Login_Service.core.workflow.AuditSink;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.port.IdentifierNormalizer;
import com.PPCloud.PP_Login_Service.port.PasswordHasher;
import com.PPCloud.PP_Login_Service.port.UserDataAccess;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * WorkflowContextFactory：把请求上下文（tenant/client/ip/ua）+ 依赖注入（dao/audit/hasher/normalizer）组装成 WorkflowContext
 */

@Service
public class WorkflowContextFactory {

    private final UserDataAccess userDao;
    private final AuditSink audit;
    private final IdentifierNormalizer normalizer;
    private final PasswordHasher passwordHasher;

    public WorkflowContextFactory(UserDataAccess userDao, AuditSink audit,
                                  IdentifierNormalizer normalizer, PasswordHasher passwordHasher) {
        this.userDao = userDao;
        this.audit = audit;
        this.normalizer = normalizer;
        this.passwordHasher = passwordHasher;
    }

    public WorkflowContext from(String tenantId, String clientId, String ip, String ua, String deviceFingerprint) {

        long timestampMillis = Instant.now().toEpochMilli();
        return new WorkflowContext(
                tenantId,
                clientId,
                ip,
                ua,
                deviceFingerprint,
                timestampMillis,
                userDao,
                audit,
                normalizer,
                passwordHasher
        );
    }
}