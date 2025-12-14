package com.PPCloud.PP_Login_Service.steps;


import com.PPCloud.PP_Login_Service.api.dto.ResetStartReq;
import com.PPCloud.PP_Login_Service.core.workflow.*;
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

        if (!(input instanceof ResetStartReq)) {
            return new StepResult.Fail("BAD_REQUEST", "INPUT_NOT_RESET_START_REQ");
        }

        FlowBag fb = new FlowBag(bag);

        int ttl = intCfg("ttlSeconds", 300);

        String type   = fb.getStr(FlowKeys.IDENTIFIER_TYPE);
        String target = fb.getStr(FlowKeys.IDENTIFIER_NORM);

        if (type == null || type.isBlank() || target == null || target.isBlank()) {
            return new StepResult.Fail("BAD_REQUEST", "MISSING_IDENTIFIER_IN_STATE");
        }

        String challengeId = "otp_" + UUID.randomUUID();
        fb.putStr(FlowKeys.OTP_CHALLENGE_ID, challengeId);

        // 可选：防止复用旧状态
        fb.putBool(FlowKeys.OTP_VERIFIED, false);

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

        return new StepResult.Halt(
                "NEED_VERIFY_CODE",
                Map.of("challengeId", challengeId, "expiresInSeconds", ttl)
        );
    }

    /** 从 cfg.params 安全取 int（支持 Integer/Long/String） */
    private int intCfg(String key, int def) {
        Object v = cfg.params().get(key);
        if (v == null) return def;
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return Math.toIntExact(l);
        if (v instanceof String s) return Integer.parseInt(s);
        throw new IllegalArgumentException("BAD_STEP_PARAM: " + key);
    }
}