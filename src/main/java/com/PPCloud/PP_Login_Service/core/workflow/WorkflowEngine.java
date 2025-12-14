package com.PPCloud.PP_Login_Service.core.workflow;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
    public WorkflowResponse resume(WorkflowContext ctx, String workflowId, String stateToken, Object input) {
        WorkflowState st = stateCodec.decode(stateToken);

        // 取你要强制继续的 workflow definition
        WorkflowDefinition def = registry.getDefinition(workflowId);

        // ✅ 关键：覆盖 workflowId 但保留 currentStepIndex + bag（不要重置为 0）
        // 同时把 version 同步成目标 workflow 的 version（更稳）
        WorkflowState patched = new WorkflowState(
                workflowId,
                def.version(),
                st.currentStepIndex(),
                st.bag()
        );

        return run(ctx, def, patched, input);
    }


    private WorkflowResponse run(WorkflowContext ctx, WorkflowDefinition def, WorkflowState state, Object input) {

        List<StepConfig> steps = def.steps();
        Map<String, Object> bag = state.bag();

        int i = state.currentStepIndex();
        while (i < steps.size()) {
            StepConfig stepCfg = steps.get(i);
            WorkflowStep step = registry.stepFactory().create(stepCfg);

            StepResult r = step.execute(ctx, bag, input);

            if (r instanceof StepResult.Ok) {
                i++; // 继续下一个节点
                state = state.withStepIndex(i);
                continue;
            }

            if (r instanceof StepResult.Halt h) {
                // HALT：生成 token，外部下一次调用 resume 才继续

                // 1) 默认：同一 workflow 的下一步继续
                WorkflowState next = state.withStepIndex(i + 1);

                // 2) ✅ 如果 step 决定“切换到另一个 workflow”，就覆盖 workflowId 并重置 stepIndex
                Object nextWfObj = bag.remove("nextWorkflowId");
                if (nextWfObj instanceof String nextWorkflowId && !nextWorkflowId.isBlank()) {

                    // 取新 workflow definition（用于拿 version，保证 token 里 version 正确）
                    WorkflowDefinition nextDef = registry.getDefinition(nextWorkflowId);

                    // 切换 workflow：workflowId 改为 nextWorkflowId，stepIndex 从 0 开始，version 同步新 def
                    next = new WorkflowState(
                            nextWorkflowId,
                            nextDef.version(),
                            0,
                            bag
                    );
                }

                String token = stateCodec.encode(next);
                return WorkflowResponse.halt(token, h.publicHintCode(), h.payload());
            }

            if (r instanceof StepResult.Fail f) {
                return WorkflowResponse.fail(f.publicHintCode(), f.reasonCode());
            }

            return WorkflowResponse.fail("INTERNAL_ERROR", "UNKNOWN_STEP_RESULT");
        }

        // 全部节点执行完，DONE
        return WorkflowResponse.done(bag);
    }
}