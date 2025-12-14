package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.api.dto.ResetCommitReq;
import com.PPCloud.PP_Login_Service.core.workflow.*;
import com.PPCloud.PP_Login_Service.port.otp.ActionTokenConsumeResult;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;
import java.util.Map;

/**
 * ActionTokenConsumeResetStep：消费 action token（一次性，原子）
 */
public class ActionTokenConsumeResetStep implements WorkflowStep {

    private final StepConfig cfg;

    public ActionTokenConsumeResetStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        if (!(input instanceof ResetCommitReq req)) {
            return new StepResult.Fail("BAD_REQUEST", "INPUT_NOT_RESET_COMMIT_REQ");
        }

        FlowBag fb = new FlowBag(bag);

        String resetToken = req.resetToken();
        if (resetToken == null || resetToken.isBlank()) {
            return new StepResult.Fail("RESET_REJECTED", "RESET_TOKEN_REQUIRED");
        }

        // TODO：真实 hash（服务端存 hash，客户端传明文）
        String tokenHash = "sha256(" + resetToken + ")";

        ActionTokenConsumeResult r = ctx.userDao.consumeActionTokenAtomically(
                ctx.tenantId, "RESET_PWD", tokenHash, ctx.now
        );

        if (!r.ok()) {
            ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_COMMIT_FAIL", r.reasonCode()));
            return new StepResult.Fail("RESET_REJECTED", r.reasonCode());
        }

        // ✅ 1) 先把 payload 放进 bag（统一 key）
        Map<String, Object> payload = (Map<String, Object>) r.payload();
        if (payload != null) {
            bag.put(FlowKeys.ACTION_TOKEN_PAYLOAD, payload);

            // ✅ 2) 如果 payload 里带了 userId，就提取出来写入统一的 USER_ID
            Object uid = payload.get("userId");
            if (uid instanceof String s && !s.isBlank()) {
                fb.putStr(FlowKeys.USER_ID, s);
                bag.put(FlowKeys.ACTION_TOKEN_USER_ID, s); // 可选：保留一份专用 key
            }
        }

        ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_COMMIT_OK", "TOKEN_CONSUMED"));

        return new StepResult.Ok(Map.of());
    }
}