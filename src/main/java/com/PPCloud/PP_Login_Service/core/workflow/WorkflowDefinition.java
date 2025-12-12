package com.PPCloud.PP_Login_Service.core.workflow;

import java.util.List;

/**
 * WorkflowDefinition：一个流程的“配置表达”
 * - workflowId：例如 WF_REGISTER_START_V1
 * - version：用于升级控制
 * - steps：节点列表（未来可完全外置成配置）
 */
public record WorkflowDefinition(
        String workflowId,
        WorkflowVersion version,
        List<StepConfig> steps
) {}