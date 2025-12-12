package com.PPCloud.PP_Login_Service.core.workflow;

/** StepFactory：按 stepType 构造 Step，实现“插件化节点库” */
public interface StepFactory {
    WorkflowStep create(StepConfig cfg);
}