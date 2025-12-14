package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.core.workflow.*;
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

        FlowBag fb = new FlowBag(bag);

        if (!fb.getBool(FlowKeys.AUTH_OK)) {
            return new StepResult.Fail("FORBIDDEN", "AUTH_NOT_OK");
        }

        String userId = fb.getStr(FlowKeys.USER_ID);
        if (userId == null || userId.isBlank()) {
            return new StepResult.Fail("INTERNAL_ERROR", "MISSING_USER_ID");
        }

        String method = fb.getStr(FlowKeys.AUTH_METHOD);
        if (method == null || method.isBlank()) method = "pwd";

        int accessTtl = intCfg("accessTtlSeconds", 3600);
        int refreshTtl = intCfg("refreshTtlSeconds", 30 * 24 * 3600);
        boolean includeRefresh = boolCfg("includeRefresh", true);

        TokenPair tp = ctx.tokenService.issueLoginTokens(new TokenIssueContext(
                ctx.tenantId,
                ctx.clientId,
                userId,
                ctx.deviceFingerprint,
                ctx.ip,
                ctx.ua,
                method,
                accessTtl,
                includeRefresh ? refreshTtl : 0
        ));

        Map<String, Object> token = new java.util.HashMap<>();
        token.put("accessToken", tp.accessToken());
        token.put("tokenType", tp.tokenType());
        token.put("expiresIn", tp.expiresIn());
        if (includeRefresh && tp.refreshToken() != null) {
            token.put("refreshToken", tp.refreshToken());
        }

        bag.put(FlowKeys.IDENTIFIER_TOKEN, token);
        return new StepResult.Ok(Map.of(
                "userId", userId,
                "token", token
        ));
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