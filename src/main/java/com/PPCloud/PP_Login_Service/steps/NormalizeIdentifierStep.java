package com.PPCloud.PP_Login_Service.steps;

import com.PPCloud.PP_Login_Service.api.dto.*;
import com.PPCloud.PP_Login_Service.core.workflow.StepConfig;
import com.PPCloud.PP_Login_Service.core.workflow.StepResult;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowStep;

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
        // input 可能是 RegisterReq/LoginIdentifyReq/ResetStartReq 等，统一用反射/接口取字段会更优雅
        // 这里为了可复制，做最朴素的 instanceof 分派。
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

        bag.put("identifierType", type);
        bag.put("identifierNorm", norm);

        Map<String, Object> payload = Map.of();
        return new StepResult.Ok(payload);
    }
}