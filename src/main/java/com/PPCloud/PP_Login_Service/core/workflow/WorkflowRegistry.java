package com.PPCloud.PP_Login_Service.core.workflow;

/**
 * WorkflowRegistry：流程定义 + StepFactory 的入口
 *
 * - 现在：FixedWorkflowRegistry（代码写死步骤）
 * - 未来：DbWorkflowRegistry（从 Mongo/PG 读取 WorkflowDefinition + steps）
 */
public interface WorkflowRegistry {
    WorkflowDefinition getDefinition(String workflowId);
    StepFactory stepFactory();
}