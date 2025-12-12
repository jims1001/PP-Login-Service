package com.PPCloud.PP_Login_Service.core.workflow;

import java.util.HashMap;
import java.util.Map;

/**
 * WorkflowState：流程运行状态（可编码成 token）
 *
 * 注意：我们不强制存“流程状态表”
 * - 需要中断续跑时，把 state 编码成 stateToken 返回给前端
 * - 前端下一次带 token 调 resume 即可
 *
 * OTP/actionToken 等“需要强一致”的状态仍存你的业务表（IamOtpChallenge/IamActionToken）
 */
public record WorkflowState(
        String workflowId,
        WorkflowVersion version,
        int currentStepIndex,
        Map<String, Object> bag
) {
    public static WorkflowState newStart(String workflowId, WorkflowVersion version) {
        return new WorkflowState(workflowId, version, 0, new HashMap<>());
    }

    public WorkflowState withCurrentStepIndex(int next) {
        return new WorkflowState(workflowId, version, next, bag);
    }
}