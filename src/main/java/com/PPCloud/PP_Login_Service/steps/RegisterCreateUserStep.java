package com.PPCloud.PP_Login_Service.steps;


import com.PPCloud.PP_Login_Service.api.dto.RegisterReq;
import com.PPCloud.PP_Login_Service.core.workflow.StepConfig;
import com.PPCloud.PP_Login_Service.core.workflow.StepResult;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowStep;
import com.PPCloud.PP_Login_Service.model.BaseModel;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;
import com.PPCloud.PP_Login_Service.model.user.IamUser;
import com.PPCloud.PP_Login_Service.model.user.IamUserIdentifier;
import com.PPCloud.PP_Login_Service.model.user.IamUserPassword;

import java.util.Map;
import java.util.Optional;

/**
 * RegisterCreateUserStep：
 * 1) 检查 identifier 是否已被占用（tenantId+type+identifier 唯一）
 * 2) 创建 IamUser
 * 3) 创建 IamUserIdentifier（verifiedAt=null）
 * 4) 如果带 password，则创建 IamUserPassword
 *
 * 结果写入 bag：
 * - userId
 */
public class RegisterCreateUserStep implements WorkflowStep {

    private final StepConfig cfg;

    public RegisterCreateUserStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        if (!(input instanceof RegisterReq req)) {
            return new StepResult.Fail("BAD_REQUEST", "INPUT_NOT_REGISTER_REQ");
        }

        String type = (String) bag.get("identifierType");
        String norm = (String) bag.get("identifierNorm");

        // identifier 已存在：拒绝（或你也可以转向登录流程，这里先拒绝）
        Optional<IamUserIdentifier> exist = ctx.userDao.findIdentifier(ctx.tenantId, type, norm);
        if (exist.isPresent()) {
            ctx.audit.append(IamAuthAudit.simple(ctx, null, "REGISTER_FAIL", "IDENTIFIER_EXISTS"));
            return new StepResult.Fail("REGISTER_REJECTED", "IDENTIFIER_EXISTS");
        }

        // 创建用户
        IamUser user = new IamUser();
        user.setTenantId(ctx.tenantId);
        user.setStatus(BaseModel.Status.ACTIVE);
        user.setDisplayName(req.displayName());
        user.setCreatedAt(ctx.now);
        user.setUpdatedAt(ctx.now);

        String userId = ctx.userDao.createUser(user);
        bag.put("userId", userId);

        // 创建 identifier
        IamUserIdentifier idf = new IamUserIdentifier();
        idf.setTenantId(ctx.tenantId);
        idf.setUserId(userId);
        idf.setType(type);
        idf.setIdentifier(norm);
        idf.setPrimary(true);
        idf.setVerifiedAt(0);
        idf.setCreatedAt(ctx.now);
        ctx.userDao.saveIdentifier(idf);

        // 可选：创建密码
        if (req.password() != null && !req.password().isBlank()) {
            String hash = ctx.passwordHasher.hash(req.password());

            IamUserPassword pwd = new IamUserPassword();
            pwd.setTenantId(ctx.tenantId);
            pwd.setUserId(userId);
            pwd.setAlgo("ARGON2ID");
            pwd.setPasswordHash(hash);
            pwd.setChangedAt(ctx.now);
            pwd.setCreatedAt(ctx.now);
            ctx.userDao.savePassword(pwd);

            bag.put("passwordEnabled", true);
        } else {
            bag.put("passwordEnabled", false);
        }

        ctx.audit.append(IamAuthAudit.simple(ctx, userId, "REGISTER_OK", "USER_CREATED"));
        Map<String, Object> payload = Map.of();
        return new StepResult.Ok(payload);
    }
}