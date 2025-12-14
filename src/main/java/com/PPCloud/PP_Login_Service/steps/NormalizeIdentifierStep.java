package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.api.dto.*;
import com.PPCloud.PP_Login_Service.core.workflow.*;

import java.util.Map;

/**
 * NormalizeIdentifierStep：规范化登录标识
 * - EMAIL/USERNAME：lowercase + trim
 * - PHONE：建议做 E.164（此处只示例）
 *
 * 输出放入 bag：
 * - identifierType
 * - identifierNorm
 */
public class NormalizeIdentifierStep implements WorkflowStep {

    private final StepConfig cfg;

    public NormalizeIdentifierStep(StepConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public StepResult execute(WorkflowContext ctx, Map<String, Object> bag, Object input) {

        FlowBag fb = new FlowBag(bag);

        // input 可能是 RegisterReq/LoginIdentifyReq/LoginPasswordReq/ResetStartReq/ResetVerifyReq 等
        String type;
        String raw;

        if (input instanceof RegisterReq r) {
            type = r.identifierType(); raw = r.identifier();
        } else if (input instanceof LoginIdentifyReq r) {
            type = r.identifierType(); raw = r.identifier();
        } else if (input instanceof LoginPasswordReq r) {
            type = r.identifierType(); raw = r.identifier();
        } else if (input instanceof ResetStartReq r) {
            type = r.identifierType(); raw = r.identifier();
        } else if (input instanceof ResetVerifyReq r) {
            type = r.identifierType(); raw = r.identifier();
        } else {
            return new StepResult.Fail("BAD_REQUEST", "UNKNOWN_INPUT_TYPE");
        }

        String norm = ctx.normalizer.normalize(type, raw);

        // ✅ 用 FlowKeys + FlowBag 写入（统一 key，不散落 magic string）
        fb.putStr(FlowKeys.IDENTIFIER_TYPE, type);
        fb.putStr(FlowKeys.IDENTIFIER_NORM, norm);

        // 如果你希望 Ok 不带 payload，可以直接 return new StepResult.Ok();
        return new StepResult.Ok(Map.of());
    }
}