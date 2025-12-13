package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.core.workflow.StepConfig;
import com.PPCloud.PP_Login_Service.core.workflow.StepResult;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowStep;
import com.PPCloud.PP_Login_Service.security.TokenIssueContext;
import com.PPCloud.PP_Login_Service.security.TokenPair;

import java.util.Map;

public class IssueAccessTokenStep implements WorkflowStep {

    private final StepConfig cfg;

    public IssueAccessTokenStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        Object ok = bag.get("authOk");
        if (!(ok instanceof Boolean) || !((Boolean) ok)) {
            return new StepResult.Fail("FORBIDDEN", "AUTH_NOT_OK");
        }

        String userId = (String) bag.get("userId");
        if (userId == null || userId.isBlank()) {
            return new StepResult.Fail("INTERNAL_ERROR", "MISSING_USER_ID");
        }

        int accessTtl = intCfg("accessTtlSeconds", 3600);
        int refreshTtl = intCfg("refreshTtlSeconds", 30 * 24 * 3600);
        boolean includeRefresh = boolCfg("includeRefresh", true);

        // ✅ 一次签发（TokenService 内部负责：JWT + Redis refresh + rotation）
        TokenPair tp = ctx.tokenService.issueLoginTokens(new TokenIssueContext(
                ctx.tenantId,
                ctx.clientId,
                userId,
                ctx.deviceFingerprint,
                ctx.ip,          // 你的 ctx 如果没有 ip/ua，就传 null 或从 bag 里取
                ctx.ua,
                "pwd",           // 或从 bag 里取，例如 bag.get("authMethod")
                accessTtl,
                includeRefresh ? refreshTtl : 0
        ));

        // ✅ 返回 payload（DONE 时 FlowResult.ok(payload) 能直接看到 token）
        Map<String, Object> token = new java.util.HashMap<>();
        token.put("accessToken", tp.accessToken());
        token.put("tokenType", tp.tokenType());
        token.put("expiresIn", tp.expiresIn());
        if (includeRefresh && tp.refreshToken() != null) {
            token.put("refreshToken", tp.refreshToken());
        }

        Map<String, Object> payload = Map.of(
                "userId", userId,
                "token", token
        );

        return new StepResult.Ok(payload); // 前提：你的 StepResult.Ok 支持 payload
    }

    private int intCfg(String key, int def) {
        Object v = cfg.params().get(key);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    private boolean boolCfg(String key, boolean def) {
        Object v = cfg.params().get(key);
        return v instanceof Boolean ? (Boolean) v : def;
    }
}