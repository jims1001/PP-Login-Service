package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.core.workflow.*;
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

        FlowBag fb = new FlowBag(bag);

        String userId = fb.getStr(FlowKeys.USER_ID);

        // ✅ otp 是否验证通过（统一 key）
        if (!fb.getBool(FlowKeys.OTP_VERIFIED)) {
            return new StepResult.Fail("REGISTER_REJECTED", "OTP_NOT_VERIFIED");
        }

        String type = fb.getStr(FlowKeys.IDENTIFIER_TYPE);
        String norm = fb.getStr(FlowKeys.IDENTIFIER_NORM);

        if (type == null || type.isBlank() || norm == null || norm.isBlank()) {
            return new StepResult.Fail("INTERNAL_ERROR", "MISSING_IDENTIFIER_IN_STATE");
        }

        ctx.userDao.markIdentifierVerified(ctx.tenantId, type, norm, ctx.now);
        ctx.audit.append(IamAuthAudit.simple(ctx, userId, "REGISTER_DONE", "IDENTIFIER_VERIFIED"));

        // ✅ 推荐：对外返回放 payload（DONE 时直接返回给前端）
        Map<String, Object> payload = Map.of("userId", userId);
        return new StepResult.Ok(payload);
    }
}