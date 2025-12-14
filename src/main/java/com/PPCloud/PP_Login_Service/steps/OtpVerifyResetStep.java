package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.api.dto.ResetVerifyReq;
import com.PPCloud.PP_Login_Service.core.workflow.*;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;
import com.PPCloud.PP_Login_Service.port.otp.OtpVerifyResult;

import java.util.Map;

/**
 * OtpVerifyResetStep：找回密码 OTP 校验（原子）
 */
public class OtpVerifyResetStep implements WorkflowStep {

    private final StepConfig cfg;

    public OtpVerifyResetStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        if (!(input instanceof ResetVerifyReq req)) {
            return new StepResult.Fail("BAD_REQUEST", "INPUT_NOT_RESET_VERIFY_REQ");
        }

        FlowBag fb = new FlowBag(bag);

        String challengeId = fb.getStr(FlowKeys.OTP_CHALLENGE_ID);
        if (challengeId == null || challengeId.isBlank()) {
            return new StepResult.Fail("VERIFY_CODE_INVALID", "CHALLENGE_ID_MISSING");
        }

        // TODO: 真实 hash
        String codeHash = "sha256(" + req.code() + ")";

        OtpVerifyResult vr = ctx.userDao.verifyOtpAtomically(ctx.tenantId, challengeId, codeHash, ctx.now);
        if (!vr.ok()) {
            ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_VERIFY_FAIL", vr.reasonCode()));
            return new StepResult.Fail("VERIFY_CODE_INVALID", vr.reasonCode());
        }

        // ✅ 写入统一 key：otp 已通过
        fb.putBool(FlowKeys.OTP_VERIFIED, true);

        ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_VERIFY_OK", "OTP_PASSED"));

        return new StepResult.Ok(Map.of());
    }
}