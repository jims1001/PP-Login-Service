package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.core.workflow.StepConfig;
import com.PPCloud.PP_Login_Service.core.workflow.StepResult;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowStep;
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

        boolean ok = Boolean.TRUE.equals(bag.get("otpVerified"));
        if (!ok) return new StepResult.Fail("RESET_REJECTED", "OTP_NOT_VERIFIED");

        int ttl = (int) cfg.params().getOrDefault("ttlSeconds", 900);

        // TODO：真实 token 生成 + hash
        String tokenPlain = "reset_" + UUID.randomUUID();
        String tokenHash = "sha256(" + tokenPlain + ")";

        IamActionToken at = new IamActionToken();
        at.setTenantId(ctx.tenantId);
        at.setType("RESET_PWD");
        at.setTokenHash(tokenHash);
        at.setPayload(Map.of("purpose", "RESET_PWD"));


        long expiresAt = ctx.now + ttl * 1000L;// 可放 identifierNorm 或 userId（取决于你策略）
        at.setExpiresAt(expiresAt);
        at.setUsedAt(0);
        at.setCreatedAt(ctx.now);

        ctx.userDao.createActionToken(at);

        ctx.audit.append(IamAuthAudit.simple(ctx, null, "RESET_TOKEN_ISSUED", "RESET_PWD"));

        // HALT：把 tokenPlain 或 tokenHash 返回给前端（生产建议只返回 tokenPlain；服务端存 hash）
        return new StepResult.Halt(
                "NEED_RESET_COMMIT",
                Map.of("resetToken", tokenPlain, "expiresInSeconds", ttl)
        );
    }
}