package com.PPCloud.PP_Login_Service.core.workflow;

/**
 * StepResult：节点执行结果
 *
 * OK   ：继续执行
 * HALT ：需要用户下一步输入（返回 stateToken + payload）
 * FAIL ：失败（publicHintCode 给前端；reasonCode 写审计/排查）
 */
public sealed interface StepResult permits StepResult.Ok, StepResult.Halt, StepResult.Fail {

    record Ok(java.util.Map<String, Object> payload) implements StepResult {}

    record Halt(String publicHintCode, Object payload) implements StepResult {}

    record Fail(String publicHintCode, String reasonCode) implements StepResult {}
}
