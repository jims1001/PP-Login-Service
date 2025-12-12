package com.PPCloud.PP_Login_Service.core.workflow;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WorkflowEngine：通用流程引擎
 *
 * - 不懂“注册/登录/找回密码”
 * - 只负责按 WorkflowDefinition 顺序执行 Step
 * - Step 可以返回三种结果：
 *    OK   ：继续下一个节点
 *    HALT ：需要用户下一步输入（例如验证码/密码），返回 stateToken
 *    FAIL ：流程失败（对外 publicHintCode；内部 reasonCode 记审计）
 */


@Service
public class WorkflowEngine {

    private final WorkflowRegistry registry;
    private final StateCodec stateCodec;

    public WorkflowEngine(WorkflowRegistry registry, StateCodec stateCodec) {
        this.registry = registry;
        this.stateCodec = stateCodec;
    }

    /** 启动流程：从第 0 个节点开始执行 */
    public WorkflowResponse start(WorkflowContext ctx, String workflowId, Object input) {
        WorkflowDefinition def = registry.getDefinition(workflowId);
        WorkflowState state = WorkflowState.newStart(def.workflowId(), def.version());
        return run(ctx, def, state, input);
    }

    /** 继续流程：根据 stateToken 中的 currentStepIndex 从指定节点继续 */
    public WorkflowResponse resume(WorkflowContext ctx, String stateToken, Object input) {
        WorkflowState state = stateCodec.decode(stateToken);
        WorkflowDefinition def = registry.getDefinition(state.workflowId());

        // 版本不一致：说明配置/代码升级了，直接失败（也可做兼容策略）
        if (!def.version().equals(state.version())) {
            return WorkflowResponse.fail("FLOW_VERSION_MISMATCH", "FLOW_VERSION_MISMATCH");
        }
        return run(ctx, def, state, input);
    }

    private WorkflowResponse run(WorkflowContext ctx, WorkflowDefinition def, WorkflowState state, Object input) {

        List<StepConfig> steps = def.steps();

        int i = state.currentStepIndex();
        while (i < steps.size()) {
            StepConfig stepCfg = steps.get(i);
            WorkflowStep step = registry.stepFactory().create(stepCfg);

            StepResult r = step.execute(ctx, state.bag(), input);

            if (r instanceof StepResult.Ok) {
                i++; // 继续下一个节点
                state = state.withCurrentStepIndex(i);
                continue;
            }

            if (r instanceof StepResult.Halt h) {
                // HALT：生成 token，外部下一次调用 resume 才继续
                WorkflowState next = state.withCurrentStepIndex(i + 1);
                String token = stateCodec.encode(next);

                return WorkflowResponse.halt(token, h.publicHintCode(), h.payload());
            }

            if (r instanceof StepResult.Fail f) {
                return WorkflowResponse.fail(f.publicHintCode(), f.reasonCode());
            }

            return WorkflowResponse.fail("INTERNAL_ERROR", "UNKNOWN_STEP_RESULT");
        }

        // 全部节点执行完，DONE
        return WorkflowResponse.done(state.bag());
    }
}