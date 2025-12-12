package com.PPCloud.PP_Login_Service.flow;

/**
 * FlowResult：对外统一返回
 * - status=OK：流程已完成
 * - status=NEED_ACTION：需要下一步（携带 flowToken）
 * - status=REJECT：失败/拒绝
 */
public record FlowResult(
        String status,          // OK | NEED_ACTION | REJECT
        String publicHintCode,
        String flowToken,       // NEED_ACTION 时返回
        Object data
) {
    public static FlowResult ok(Object data) {
        return new FlowResult("OK", "OK", null, data);
    }
    public static FlowResult needAction(String token, String hint, Object data) {
        return new FlowResult("NEED_ACTION", hint, token, data);
    }
    public static FlowResult reject(String hint) {
        return new FlowResult("REJECT", hint, null, null);
    }
}