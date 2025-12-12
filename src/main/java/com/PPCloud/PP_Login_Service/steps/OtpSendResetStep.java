package com.PPCloud.PP_Login_Service.steps;


import com.PPCloud.PP_Login_Service.api.dto.ResetStartReq;
import com.PPCloud.PP_Login_Service.core.workflow.StepConfig;
import com.PPCloud.PP_Login_Service.core.workflow.StepResult;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowStep;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;
import com.PPCloud.PP_Login_Service.model.user.IamOtpChallenge;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * OtpSendResetStep：找回密码发送 OTP
 * - 同样要防枚举：不存在的账号也返回 NEED_ACTION（但验证码校验会失败）
 */
public class OtpSendResetStep implements WorkflowStep {

    private final StepConfig cfg;

    public OtpSendResetStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        if (!(input instanceof ResetStartReq req)) {
            return new StepResult.Fail("BAD_REQUEST", "INPUT_NOT_RESET_START_REQ");
        }

        int ttl = (int) cfg.params().getOrDefault("ttlSeconds", 300);

        String type = (String) bag.get("identifierType");
        String target = (String) bag.get("identifierNorm");

        String challengeId = "otp_" + UUID.randomUUID();
        bag.put("challengeId", challengeId);

        String code = "123456"; // TODO random
        String codeHash = "sha256(" + code + ")";

        IamOtpChallenge otp = new IamOtpChallenge();
        otp.setId(challengeId);
        otp.setTenantId(ctx.tenantId);
        otp.setPurpose("RESET_PASSWORD");
        otp.setChannel("EMAIL".equals(type) ? "EMAIL" : "SMS");
        otp.setTarget(target);
        otp.setCodeHash(codeHash);
        otp.setAttempts(0);
        otp.setMaxAttempts(5);


        long expiresAtMs = ctx.now + ttl * 1000L;
        otp.setExpiresAt(expiresAtMs);
        otp.setCreatedAt(ctx.now);

        ctx.userDao.createOtp(otp);

        ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_OTP_SENT", "RESET_PASSWORD"));

        return new StepResult.Halt("NEED_VERIFY_CODE", Map.of("challengeId", challengeId, "expiresInSeconds", ttl));
    }
}