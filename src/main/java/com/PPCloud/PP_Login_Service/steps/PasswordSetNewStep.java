package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.api.dto.ResetCommitReq;
import com.PPCloud.PP_Login_Service.core.workflow.*;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;
import com.PPCloud.PP_Login_Service.model.user.IamUserPassword;

import java.util.Map;

/**
 * PasswordSetNewStep：设置新密码
 * - 需要从 actionTokenPayload 决定 userId（这里示例用 req.userId，你可改成 payload）
 */
public class PasswordSetNewStep implements WorkflowStep {

    private final StepConfig cfg;

    public PasswordSetNewStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        if (!(input instanceof ResetCommitReq req)) {
            return new StepResult.Fail("BAD_REQUEST", "INPUT_NOT_RESET_COMMIT_REQ");
        }

        FlowBag fb = new FlowBag(bag);

        // ✅ 临时兼容：userId 先从 req 取（你后续会改成 actionToken 解出来）
        String userId = req.userId();
        if (userId == null || userId.isBlank()) {
            return new StepResult.Fail("RESET_REJECTED", "USER_ID_REQUIRED");
        }

        String hash = ctx.passwordHasher.hash(req.newPassword());

        IamUserPassword pwd = new IamUserPassword();
        pwd.setTenantId(ctx.tenantId);
        pwd.setUserId(userId);
        pwd.setAlgo("ARGON2ID");
        pwd.setPasswordHash(hash);
        pwd.setChangedAt(ctx.now);
        pwd.setCreatedAt(ctx.now);

        ctx.userDao.savePassword(pwd);
        ctx.audit.append(IamAuthAudit.simple(ctx, userId, "RESET_DONE", "PASSWORD_UPDATED"));

        // ✅ 推荐：对外返回放 payload，不污染 bag
        Map<String, Object> payload = Map.of(
                "userId", userId,
                "passwordUpdated", true
        );

        return new StepResult.Ok(payload);
    }
}
