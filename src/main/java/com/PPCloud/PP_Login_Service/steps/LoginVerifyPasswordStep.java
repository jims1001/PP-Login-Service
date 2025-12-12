package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.api.dto.LoginPasswordReq;
import com.PPCloud.PP_Login_Service.core.workflow.StepConfig;
import com.PPCloud.PP_Login_Service.core.workflow.StepResult;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowStep;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;
import com.PPCloud.PP_Login_Service.model.user.IamUserIdentifier;
import com.PPCloud.PP_Login_Service.model.user.IamUserPassword;

import java.util.Map;
import java.util.Optional;

/**
 * LoginVerifyPasswordStep：校验密码
 *
 * - 防枚举：identifier 不存在也走同样的失败提示码
 * - 成功：在 bag 写入 authOk=true
 */
public class LoginVerifyPasswordStep implements WorkflowStep {

    private final StepConfig cfg;

    public LoginVerifyPasswordStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        if (!(input instanceof LoginPasswordReq req)) {
            return new StepResult.Fail("BAD_REQUEST", "INPUT_NOT_LOGIN_PASSWORD_REQ");
        }

        String type = (String) bag.get("identifierType");
        String norm = (String) bag.get("identifierNorm");

        Optional<IamUserIdentifier> idfOpt = ctx.userDao.findIdentifier(ctx.tenantId, type, norm);
        if (idfOpt.isEmpty()) {
            ctx.audit.append(IamAuthAudit.simple(ctx, null, "LOGIN_FAIL", "IDENTIFIER_NOT_FOUND"));
            return new StepResult.Fail("LOGIN_FAILED", "IDENTIFIER_NOT_FOUND");
        }

        String userId = idfOpt.get().getUserId();
        bag.put("userId", userId);

        Optional<IamUserPassword> pwdOpt = ctx.userDao.findPassword(ctx.tenantId, userId);
        if (pwdOpt.isEmpty()) {
            ctx.audit.append(IamAuthAudit.simple(ctx, userId, "LOGIN_FAIL", "PASSWORD_NOT_SET"));
            return new StepResult.Fail("LOGIN_FAILED", "PASSWORD_NOT_SET");
        }

        IamUserPassword pwd = pwdOpt.get();
        boolean ok = ctx.passwordHasher.verify(req.password(), pwd.getPasswordHash());

        if (!ok) {
            ctx.userDao.bumpPasswordFail(ctx.tenantId, userId, ctx.now);
            ctx.audit.append(IamAuthAudit.simple(ctx, userId, "LOGIN_FAIL", "BAD_PASSWORD"));
            return new StepResult.Fail("LOGIN_FAILED", "BAD_PASSWORD");
        }

        ctx.userDao.clearPasswordFail(ctx.tenantId, userId, ctx.now);
        ctx.audit.append(IamAuthAudit.simple(ctx, userId, "LOGIN_OK", "PWD_OK"));

        bag.put("authOk", true);
        bag.put("result", Map.of("userId", userId, "issueToken", true)); // 你后续可接发 token
        return new StepResult.Ok();
    }
}