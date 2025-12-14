package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.core.workflow.*;
import com.PPCloud.PP_Login_Service.model.user.IamActionToken;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ActionTokenIssueResetStep：
 * - OTP 通过后，签发一个一次性 action token（RESET_PWD）
 * - 返回 HALT，要求前端下一步带 tokenHash + newPassword 调 /reset/commit
 */
public class ActionTokenIssueResetStep implements WorkflowStep {

    private final StepConfig cfg;

    public ActionTokenIssueResetStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        FlowBag fb = new FlowBag(bag);

        // ✅ 必须先 OTP 校验通过
        if (!fb.getBool(FlowKeys.OTP_VERIFIED)) {
            return new StepResult.Fail("RESET_REJECTED", "OTP_NOT_VERIFIED");
        }

        int ttl = intCfg("ttlSeconds", 900);

        // TODO：真实 token 生成 + hash（服务端只存 hash）
        String tokenPlain = "reset_" + UUID.randomUUID();
        String tokenHash  = "sha256(" + tokenPlain + ")";

        IamActionToken at = new IamActionToken();
        at.setTenantId(ctx.tenantId);
        at.setType("RESET_PWD");
        at.setTokenHash(tokenHash);

        // ✅ payload 建议绑定主体（至少绑定 identifierNorm 或 userId）
        // 这样 resetToken 被窃取后也更难跨账号滥用（后续 consume 时核对）
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("purpose", "RESET_PWD");

        String idfNorm = fb.getStr(FlowKeys.IDENTIFIER_NORM);
        if (idfNorm != null && !idfNorm.isBlank()) payload.put("identifierNorm", idfNorm);

        String userId = fb.getStr(FlowKeys.USER_ID);
        if (userId != null && !userId.isBlank()) payload.put("userId", userId);

        at.setPayload(payload);

        long expiresAt = ctx.now + ttl * 1000L;
        at.setExpiresAt(expiresAt);
        at.setUsedAt(0);
        at.setCreatedAt(ctx.now);

        ctx.userDao.createActionToken(at);

        ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_TOKEN_ISSUED", "RESET_PWD"));

        // ✅ HALT：返回给前端明文 token（服务端存 hash）
        return new StepResult.Halt(
                "NEED_RESET_COMMIT",
                Map.of(
                        "resetToken", tokenPlain,
                        "expiresInSeconds", ttl
                )
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