package com.PPCloud.PP_Login_Service.core.workflow;

import java.util.Map;

/**
 * WorkflowStep：流程节点（工作节点）
 * - ctx：请求上下文（tenant/client/ip/ua/dao/audit/now 等）
 * - bag：流程内共享状态（可序列化，支持中断后续跑）
 * - input：本次请求输入（start/resume 传入）
 */
public interface WorkflowStep {
    StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input);
}