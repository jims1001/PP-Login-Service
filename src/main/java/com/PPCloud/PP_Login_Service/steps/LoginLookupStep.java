package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.api.dto.LoginIdentifyReq;
import com.PPCloud.PP_Login_Service.core.workflow.*;
import com.PPCloud.PP_Login_Service.model.BaseModel;
import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;
import com.PPCloud.PP_Login_Service.model.user.IamUser;
import com.PPCloud.PP_Login_Service.model.user.IamUserFactor;
import com.PPCloud.PP_Login_Service.model.user.IamUserIdentifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * LoginLookupStep：按 identifier 查用户并给出 nextAction
 *
 * - 防枚举：identifier 不存在也返回同样结构（publicHintCode=CONTINUE）
 * - nextAction：PWD / OTP / REJECT（你以后可扩展 STEP_UP/MFA_REQUIRED）
 *
 * 输出：
 * - bag.userId（若存在）
 * - payload.nextAction
 * - HALT（等待用户提交密码或 OTP）
 */
public class LoginLookupStep implements WorkflowStep {

    private final StepConfig cfg;

    public LoginLookupStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        if (!(input instanceof LoginIdentifyReq)) {
            return new StepResult.Fail("BAD_REQUEST", "INPUT_NOT_LOGIN_IDENTIFY_REQ");
        }

        FlowBag fb = new FlowBag(bag);

        // ✅ 从 FlowKeys 取规范化后的 identifier
        String type = fb.getStr(FlowKeys.IDENTIFIER_TYPE);
        String norm = fb.getStr(FlowKeys.IDENTIFIER_NORM);

        if (type == null || type.isBlank() || norm == null || norm.isBlank()) {
            return new StepResult.Fail("BAD_REQUEST", "MISSING_IDENTIFIER_IN_STATE");
        }

        Optional<IamUserIdentifier> idfOpt = ctx.userDao.findIdentifier(ctx.tenantId, type, norm);
        if (idfOpt.isEmpty()) {
            // 防枚举：不暴露不存在
            ctx.audit.append(IamAuthAudit.simple(ctx, null, "LOGIN_IDENTIFY", "IDENTIFIER_NOT_FOUND"));
            return new StepResult.Halt("CONTINUE", Map.of("nextAction", "PWD"));
        }

        IamUserIdentifier idf = idfOpt.get();

        Optional<IamUser> userOpt = ctx.userDao.findUser(ctx.tenantId, idf.getUserId());
        if (userOpt.isEmpty()) {
            ctx.audit.append(IamAuthAudit.simple(ctx, null, "LOGIN_IDENTIFY", "USER_NOT_FOUND"));
            return new StepResult.Halt("CONTINUE", Map.of("nextAction", "PWD"));
        }

        IamUser user = userOpt.get();



        if (!BaseModel.Status.ACTIVE.equals(user.getStatus())) {
            ctx.audit.append(IamAuthAudit.simple(ctx, user.getId(), "LOGIN_IDENTIFY", "USER_NOT_ACTIVE"));
            return new StepResult.Fail("LOGIN_REJECTED", "USER_NOT_ACTIVE");
        }

        // ✅ 写入 userId（统一 key）
        fb.putStr(FlowKeys.USER_ID, user.getId());

        boolean hasPwd = ctx.userDao.findPassword(ctx.tenantId, user.getId()).isPresent();
        List<IamUserFactor> factors = ctx.userDao.listEnabledFactors(ctx.tenantId, user.getId());
        boolean hasFactor = factors != null && !factors.isEmpty();

        String nextAction = hasPwd ? "PWD" : (hasFactor ? "OTP" : "REJECT");

        ctx.audit.append(IamAuthAudit.simple(ctx, user.getId(), "LOGIN_IDENTIFY", "NEXT_" + nextAction));

        // HALT：让前端按 nextAction 走下一步（例如 /login/password）
        return new StepResult.Halt("CONTINUE", Map.of("nextAction", nextAction));
    }
}