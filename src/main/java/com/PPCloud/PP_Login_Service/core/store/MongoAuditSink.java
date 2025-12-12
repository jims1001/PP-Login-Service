package com.PPCloud.PP_Login_Service.core.store;

import com.PPCloud.PP_Login_Service.Repository.IamAuthAuditRepository;
import com.PPCloud.PP_Login_Service.core.workflow.AuditSink;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;
import org.springframework.stereotype.Service;

/**
 * AuditSink：审计落库适配层
 * - workflow/steps 只依赖 AuditSink
 * - 当前实现：直接写 Mongo（IamAuthAuditRepository）
 * - 未来换库：换实现即可
 */
@Service
public class MongoAuditSink implements AuditSink {

    private final IamAuthAuditRepository repo;

    public MongoAuditSink(IamAuthAuditRepository repo) {
        this.repo = repo;
    }

    @Override
    public void append(IamAuthAudit audit) {
        repo.save(audit);
    }
}
