package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.core.workflow.*;
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

        FlowBag fb = new FlowBag(bag);

        String userId = fb.getStr(FlowKeys.USER_ID);
        String type   = fb.getStr(FlowKeys.IDENTIFIER_TYPE);
        String target = fb.getStr(FlowKeys.IDENTIFIER_NORM);

        if (type == null || type.isBlank() || target == null || target.isBlank()) {
            return new StepResult.Fail("BAD_REQUEST", "MISSING_IDENTIFIER_IN_STATE");
        }

        // cfg.params() 里的数字可能是 Integer/Long，别直接 (int) 强转
        int ttl = intCfg("ttlSeconds", 300);

        String challengeId = "otp_" + UUID.randomUUID();
        fb.putStr(FlowKeys.OTP_CHALLENGE_ID, challengeId);

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

        // TODO: 实际发送短信/邮件
        ctx.audit.append(IamAuthAudit.simple(ctx, userId, "OTP_SENT", "REGISTER_VERIFY"));

        return new StepResult.Halt(
                "NEED_VERIFY_CODE",
                Map.of(
                        "challengeId", challengeId,
                        "expiresInSeconds", ttl
                )
        );
    }

    /** 从 cfg.params 里安全取 int（避免 Integer/Long/String 强转异常） */
    private int intCfg(String key, int def) {
        Object v = cfg.params().get(key);
        if (v == null) return def;
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return Math.toIntExact(l);
        if (v instanceof String s) return Integer.parseInt(s);
        throw new IllegalArgumentException("BAD_STEP_PARAM: " + key);
    }
}