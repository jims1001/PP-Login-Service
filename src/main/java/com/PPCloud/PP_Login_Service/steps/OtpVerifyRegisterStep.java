package com.PPCloud.PP_Login_Service.steps;
import com.PPCloud.PP_Login_Service.api.dto.RegisterVerifyReq;
import com.PPCloud.PP_Login_Service.core.workflow.*;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;
import com.PPCloud.PP_Login_Service.port.otp.OtpVerifyResult;

import java.util.Map;

/**
 * OtpVerifyRegisterStep：校验 OTP（必须原子）
 * - 校验成功：在 bag 写入 otpVerified=true
 * - 校验失败：FAIL（publicHintCode 不暴露具体原因）
 */
public class OtpVerifyRegisterStep implements WorkflowStep {

    private final StepConfig cfg;

    public OtpVerifyRegisterStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        if (!(input instanceof RegisterVerifyReq req)) {
            return new StepResult.Fail("BAD_REQUEST", "INPUT_NOT_REGISTER_VERIFY_REQ");
        }

        FlowBag fb = new FlowBag(bag);

        String userId = fb.getStr(FlowKeys.USER_ID);
        String challengeId = fb.getStr(FlowKeys.OTP_CHALLENGE_ID);

        if (challengeId == null || challengeId.isBlank()) {
            // 没有 challengeId：说明 stateToken 被篡改 / 流程不匹配 / 没走过 sendOtp
            return new StepResult.Fail("VERIFY_CODE_INVALID", "CHALLENGE_ID_MISSING");
        }

        // TODO: 真实 hash（不要 sha256("xxxx") 这种字符串）
        String codeHash = "sha256(" + req.code() + ")";

        OtpVerifyResult vr = ctx.userDao.verifyOtpAtomically(ctx.tenantId, challengeId, codeHash, ctx.now);
        if (!vr.ok()) {
            ctx.audit.append(IamAuthAudit.simple(ctx, userId, "REGISTER_VERIFY_FAIL", vr.reasonCode()));
            return new StepResult.Fail("VERIFY_CODE_INVALID", vr.reasonCode());
        }

        // ✅ 写入统一 key：otp 已通过
        fb.putBool(FlowKeys.OTP_VERIFIED, true);

        ctx.audit.append(IamAuthAudit.simple(ctx, userId, "REGISTER_VERIFY_OK", "OTP_PASSED"));

        return new StepResult.Ok(Map.of());
    }
}