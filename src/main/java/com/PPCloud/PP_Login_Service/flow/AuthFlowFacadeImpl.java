package com.PPCloud.PP_Login_Service.flow;

import com.PPCloud.PP_Login_Service.api.dto.*;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowEngine;
import com.PPCloud.PP_Login_Service.core.workflow.WorkflowResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * AuthFlowFacadeImpl：外部业务接口 → 内部 workflow start/resume
 *
 * 对外仍是：注册/登录/找回密码
 * 对内统一：workflow 引擎 + 固定（未来可配置）节点编排
 */

@Service
public class AuthFlowFacadeImpl implements AuthFlowFacade {

    private final WorkflowEngine engine;
    private final WorkflowContextFactory contextFactory;
    private final WorkflowResolver resolver;

    public AuthFlowFacadeImpl(
            WorkflowEngine engine,
            WorkflowContextFactory contextFactory,
            WorkflowResolver resolver
    ) {
        this.engine = engine;
        this.contextFactory = contextFactory;
        this.resolver = resolver;
    }

    @Override
    public FlowResult register(RegisterReq req) {
        WorkflowContext ctx = contextFactory.from(
                req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint()
        );

        // ✅ 关键：按 tenantId + clientId 解析“注册 start”流程
        String workflowId = resolver.resolveWorkflowId(req.tenantId(), req.clientId(), FlowKind.REGISTER_START);

        WorkflowResponse wr = engine.start(ctx, workflowId, req);
        return toFlowResult(wr);
    }

    @Override
    public FlowResult registerVerify(RegisterVerifyReq req) {
        WorkflowContext ctx = contextFactory.from(
                req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint()
        );

        // ✅ 强制走 VERIFY workflow（不依赖 token 里的 workflowId）
        WorkflowResponse wr = engine.resume(ctx, "WF_REGISTER_VERIFY_V1", req.flowToken(), req);

        return toFlowResult(wr);
    }

    @Override
    public FlowResult loginIdentify(LoginIdentifyReq req) {
        WorkflowContext ctx = contextFactory.from(
                req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint()
        );

        // ✅ 关键：按 tenantId + clientId 解析“登录 identify”流程
        String workflowId = resolver.resolveWorkflowId(req.tenantId(), req.clientId(), FlowKind.LOGIN_IDENTIFY);

        WorkflowResponse wr = engine.start(ctx, workflowId, req);
        return toFlowResult(wr);
    }

    @Override
    public FlowResult loginPassword(LoginPasswordReq req) {
        WorkflowContext ctx = contextFactory.from(
                req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint()
        );

        // ✅ 关键：按 clientId 决定走 V1 还是 V2
        // 比如：admin 走 V2（登录后返回 accessToken），普通走 V1（不发 token）
        String wfId = resolver.resolveWorkflowId(req.tenantId(), req.clientId(), FlowKind.LOGIN_PASSWORD);

        // ✅ 用“带 workflowId 的 resume”
        WorkflowResponse wr = engine.resume(ctx, wfId, req.flowToken(), req);

        return toFlowResult(wr);
    }

    @Override
    public FlowResult resetStart(ResetStartReq req) {
        WorkflowContext ctx = contextFactory.from(
                req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint()
        );

        // ✅ 关键：按 tenantId + clientId 解析“找回密码 start”流程
        String workflowId = resolver.resolveWorkflowId(req.tenantId(), req.clientId(), FlowKind.RESET_START);

        WorkflowResponse wr = engine.start(ctx, workflowId, req);
        return toFlowResult(wr);
    }

    @Override
    public FlowResult resetVerify(ResetVerifyReq req) {
        WorkflowContext ctx = contextFactory.from(
                req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint()
        );

        // ✅ 强制走 WF_RESET_VERIFY_V1
        WorkflowResponse wr = engine.resume(ctx, "WF_RESET_VERIFY_V1", req.flowToken(), req);

        return toFlowResult(wr);
    }

    @Override
    public FlowResult resetCommit(ResetCommitReq req) {
        WorkflowContext ctx = contextFactory.from(
                req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint()
        );

        // ✅ 强制走 COMMIT workflow
        WorkflowResponse wr = engine.resume(ctx, "WF_RESET_COMMIT_V1", req.flowToken(), req);

        return toFlowResult(wr);
    }

    private FlowResult toFlowResult(WorkflowResponse wr) {
        return switch (wr.status()) {
            case "DONE" -> FlowResult.ok(wr.payload() != null ? wr.payload() : wr.bag());
            case "HALT" -> FlowResult.needAction(wr.stateToken(), wr.publicHintCode(), wr.payload());
            default -> FlowResult.reject(wr.publicHintCode());
        };
    }
}