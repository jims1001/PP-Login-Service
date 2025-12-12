package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.api.dto.ResetVerifyReq;
import com.PPCloud.PP_Login_Service.core.workflow.StepConfig;
import com.PPCloud.PP_Login_Service.core.workflow.StepResult;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowStep;
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

        String challengeId = (String) bag.get("challengeId");
        if (challengeId == null) return new StepResult.Fail("VERIFY_CODE_INVALID", "CHALLENGE_ID_MISSING");

        String codeHash = "sha256(" + req.code() + ")";

        OtpVerifyResult vr = ctx.userDao.verifyOtpAtomically(ctx.tenantId, challengeId, codeHash, ctx.now);
        if (!vr.ok()) {
            ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_VERIFY_FAIL", vr.reasonCode()));
            return new StepResult.Fail("VERIFY_CODE_INVALID", vr.reasonCode());
        }

        bag.put("otpVerified", true);
        ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_VERIFY_OK", "OTP_PASSED"));
        return new StepResult.Ok();
    }
}