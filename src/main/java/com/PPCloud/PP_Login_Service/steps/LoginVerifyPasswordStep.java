package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.api.dto.LoginPasswordReq;
import com.PPCloud.PP_Login_Service.core.workflow.*;
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

        FlowBag fb = new FlowBag(bag);

        // ✅ 从 FlowKeys 读取（而不是 magic string）
        String type = fb.getStr(FlowKeys.IDENTIFIER_TYPE);
        String norm = fb.getStr(FlowKeys.IDENTIFIER_NORM);

        if (type == null || type.isBlank() || norm == null || norm.isBlank()) {
            return new StepResult.Fail("BAD_REQUEST", "MISSING_IDENTIFIER_IN_STATE");
        }

        Optional<IamUserIdentifier> idfOpt = ctx.userDao.findIdentifier(ctx.tenantId, type, norm);
        if (idfOpt.isEmpty()) {
            ctx.audit.append(IamAuthAudit.simple(ctx, null, "LOGIN_FAIL", "IDENTIFIER_NOT_FOUND"));
            return new StepResult.Fail("LOGIN_FAILED", "IDENTIFIER_NOT_FOUND");
        }

        String userId = idfOpt.get().getUserId();

        // ✅ 写入 userId（统一 key）
        fb.putStr(FlowKeys.USER_ID, userId);

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

        // ✅ 标记认证通过（统一 key）
        fb.putBool(FlowKeys.AUTH_OK, true);

        // 可选：如果你真的要把“是否发 token”这种提示留在 bag（一般建议放 payload）
        // fb.putMap("result", Map.of("userId", userId, "issueToken", true));

        // ✅ 建议：把对外需要的东西放 payload（DONE 时回给前端）
        Map<String, Object> payload = Map.of(
                "userId", userId,
                "issueToken", true
        );

        return new StepResult.Ok(payload);
    }
}