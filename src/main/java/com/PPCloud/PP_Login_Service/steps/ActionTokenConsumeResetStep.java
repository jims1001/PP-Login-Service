package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.api.dto.ResetCommitReq;
import com.PPCloud.PP_Login_Service.core.workflow.StepConfig;
import com.PPCloud.PP_Login_Service.core.workflow.StepResult;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowStep;
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

        // TODO：把 token 明文 hash 后再查库
        String tokenHash = "sha256(" + req.resetToken() + ")";

        ActionTokenConsumeResult r = ctx.userDao.consumeActionTokenAtomically(ctx.tenantId, "RESET_PWD", tokenHash, ctx.now);
        if (!r.ok()) {
            ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_COMMIT_FAIL", r.reasonCode()));
            return new StepResult.Fail("RESET_REJECTED", r.reasonCode());
        }

        // payload 里可以拿到 userId/identifier（取决于你 token payload 存啥）
        bag.put("actionTokenPayload", r.payload());
        ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_COMMIT_OK", "TOKEN_CONSUMED"));
        Map<String, Object> payload = Map.of();
        return new StepResult.Ok(payload);
    }
}