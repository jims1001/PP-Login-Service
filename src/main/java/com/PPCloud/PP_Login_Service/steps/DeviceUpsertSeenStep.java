package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.core.workflow.*;

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

        FlowBag fb = new FlowBag(bag);

        // ✅ 统一从 FlowKeys 读认证结果
        if (!fb.getBool(FlowKeys.AUTH_OK)) {
            return new StepResult.Fail("LOGIN_FAILED", "AUTH_NOT_OK");
        }

        // ✅ 统一从 FlowKeys 读 userId
        String userId = fb.getStr(FlowKeys.USER_ID);
        if (userId == null || userId.isBlank()) {
            return new StepResult.Fail("LOGIN_FAILED", "USER_ID_MISSING");
        }

        // 平台字段你可以扩展：platform / os / appVersion / ip 等
        ctx.userDao.upsertDeviceSeen(
                ctx.tenantId,
                userId,
                ctx.deviceFingerprint,
                ctx.ua,
                "unknown",
                ctx.now
        );

        return new StepResult.Ok(Map.of());
    }
}