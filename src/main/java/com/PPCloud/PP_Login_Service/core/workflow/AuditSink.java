package com.PPCloud.PP_Login_Service.core.workflow;

import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;

public interface AuditSink {
    void append(IamAuthAudit audit);
}