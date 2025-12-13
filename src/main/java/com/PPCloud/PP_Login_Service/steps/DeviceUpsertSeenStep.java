package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.core.workflow.StepConfig;
import com.PPCloud.PP_Login_Service.core.workflow.StepResult;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowStep;

import java.util.Map;

/**
 * DeviceUpsertSeenStep：登录成功后记录设备（风控基础）
 */
public class DeviceUpsertSeenStep implements WorkflowStep {

    private final StepConfig cfg;

    public DeviceUpsertSeenStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {
        boolean ok = Boolean.TRUE.equals(bag.get("authOk"));
        if (!ok) return new StepResult.Fail("LOGIN_FAILED", "AUTH_NOT_OK");

        String userId = (String) bag.get("userId");
        if (userId == null) return new StepResult.Fail("LOGIN_FAILED", "USER_ID_MISSING");

        // 平台字段你可以自己完善（这里 platform 简化）
        ctx.userDao.upsertDeviceSeen(ctx.tenantId, userId, ctx.deviceFingerprint, ctx.ua, "unknown", ctx.now);
        Map<String, Object> payload = Map.of();
        return new StepResult.Ok(payload);
    }
}