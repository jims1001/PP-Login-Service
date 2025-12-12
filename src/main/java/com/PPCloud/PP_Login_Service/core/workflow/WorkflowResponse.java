package com.PPCloud.PP_Login_Service.core.workflow;

import java.util.Map;

/**
 * WorkflowResponse：引擎对外返回（被 Facade 转成业务 FlowResult）
 */
public record WorkflowResponse(
        String status,          // DONE | HALT | FAIL
        String stateToken,      // HALT 时返回
        String publicHintCode,  // 给前端
        String reasonCode,      // 仅 FAIL 时内部原因
        Object payload,         // HALT 时携带 challengeId/nextAction 等
        Map<String, Object> bag // DONE 时可返回（调试/内部）
) {
    public static WorkflowResponse halt(String token, String hint, Object payload) {
        return new WorkflowResponse("HALT", token, hint, null, payload, null);
    }
    public static WorkflowResponse done(Map<String, Object> bag) {
        return new WorkflowResponse("DONE", null, "OK", null, bag, bag);
    }
    public static WorkflowResponse fail(String hint, String reason) {
        return new WorkflowResponse("FAIL", null, hint, reason, null, null);
    }
}