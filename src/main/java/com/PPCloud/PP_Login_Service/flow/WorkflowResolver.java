package com.PPCloud.PP_Login_Service.flow;

public interface WorkflowResolver {
    String resolveWorkflowId(String tenantId, String clientId, FlowKind kind);
}