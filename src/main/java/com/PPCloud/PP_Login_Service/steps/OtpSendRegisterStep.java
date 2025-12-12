package com.PPCloud.PP_Login_Service.steps;

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
 * OtpSendRegisterStep：
 * 1) 生成 OTP challenge（保存 IamOtpChallenge）
 * 2) 返回 HALT，要求用户下一步提交 code
 *
 * payload 返回给前端：challengeId + expiresIn
 */
public class OtpSendRegisterStep implements WorkflowStep {

    private final StepConfig cfg;

    public OtpSendRegisterStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        String userId = (String) bag.get("userId");
        String type = (String) bag.get("identifierType");
        String target = (String) bag.get("identifierNorm");

        int ttl = (int) cfg.params().getOrDefault("ttlSeconds", 300);

        String challengeId = "otp_" + UUID.randomUUID();
        bag.put("challengeId", challengeId);

        // TODO: 真实随机验证码 + hash（不要明文存库）
        String code = "123456";
        String codeHash = "sha256(" + code + ")";



        long expiresAtMs = ctx.now + ttl * 1000L;

        IamOtpChallenge otp = new IamOtpChallenge();
        otp.setId(challengeId);
        otp.setTenantId(ctx.tenantId);
        otp.setPurpose("REGISTER_VERIFY");
        otp.setChannel("EMAIL".equals(type) ? "EMAIL" : "SMS");
        otp.setTarget(target);
        otp.setCodeHash(codeHash);
        otp.setAttempts(0);
        otp.setMaxAttempts(5);
        otp.setExpiresAt(expiresAtMs);
        otp.setPassedAt(0);
        otp.setCreatedAt(ctx.now);

        ctx.userDao.createOtp(otp);

        // TODO: 实际发送短信/邮件，这里只做结构示例
        ctx.audit.append(IamAuthAudit.simple(ctx, userId, "OTP_SENT", "REGISTER_VERIFY"));

        return new StepResult.Halt(
                "NEED_VERIFY_CODE",
                Map.of("challengeId", challengeId, "expiresInSeconds", ttl)
        );
    }
}