package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.core.workflow.StepConfig;
import com.PPCloud.PP_Login_Service.core.workflow.StepResult;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowStep;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;

import java.util.Map;

/**
 * IdentifierMarkVerifiedStep：把 IamUserIdentifier.verifiedAt 写入
 * - 只有 otpVerified=true 才允许继续（防止跳步）
 */
public class IdentifierMarkVerifiedStep implements WorkflowStep {

    private final StepConfig cfg;

    public IdentifierMarkVerifiedStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        String userId = (String) bag.get("userId");
        boolean ok = Boolean.TRUE.equals(bag.get("otpVerified"));
        if (!ok) return new StepResult.Fail("REGISTER_REJECTED", "OTP_NOT_VERIFIED");

        String type = (String) bag.get("identifierType");
        String norm = (String) bag.get("identifierNorm");

        ctx.userDao.markIdentifierVerified(ctx.tenantId, type, norm, ctx.now);
        ctx.audit.append(IamAuthAudit.simple(ctx, userId, "REGISTER_DONE", "IDENTIFIER_VERIFIED"));

        // DONE 后对外返回数据（bag 会被引擎作为 DONE payload）
        bag.put("result", Map.of("userId", userId));
        return new StepResult.Ok();
    }
}