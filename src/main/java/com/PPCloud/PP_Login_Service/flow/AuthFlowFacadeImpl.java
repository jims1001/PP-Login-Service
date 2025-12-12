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

    public AuthFlowFacadeImpl(WorkflowEngine engine, WorkflowContextFactory contextFactory) {
        this.engine = engine;
        this.contextFactory = contextFactory;
    }

    @Override
    public FlowResult register(RegisterReq req) {
        WorkflowContext ctx = contextFactory.from(req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint());
        WorkflowResponse wr = engine.start(ctx, "WF_REGISTER_START_V1", req);
        return toFlowResult(wr);
    }

    @Override
    public FlowResult registerVerify(RegisterVerifyReq req) {
        WorkflowContext ctx = contextFactory.from(req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint());
        WorkflowResponse wr = engine.resume(ctx, req.flowToken(), req);
        return toFlowResult(wr);
    }

    @Override
    public FlowResult loginIdentify(LoginIdentifyReq req) {
        WorkflowContext ctx = contextFactory.from(req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint());
        WorkflowResponse wr = engine.start(ctx, "WF_LOGIN_IDENTIFY_V1", req);
        return toFlowResult(wr);
    }

    @Override
    public FlowResult loginPassword(LoginPasswordReq req) {
        WorkflowContext ctx = contextFactory.from(req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint());
        WorkflowResponse wr = engine.resume(ctx, req.flowToken(), req);
        return toFlowResult(wr);
    }

    @Override
    public FlowResult resetStart(ResetStartReq req) {
        WorkflowContext ctx = contextFactory.from(req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint());
        WorkflowResponse wr = engine.start(ctx, "WF_RESET_START_V1", req);
        return toFlowResult(wr);
    }

    @Override
    public FlowResult resetVerify(ResetVerifyReq req) {
        WorkflowContext ctx = contextFactory.from(req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint());
        WorkflowResponse wr = engine.resume(ctx, req.flowToken(), req);
        return toFlowResult(wr);
    }

    @Override
    public FlowResult resetCommit(ResetCommitReq req) {
        WorkflowContext ctx = contextFactory.from(req.tenantId(), req.clientId(), req.ip(), req.ua(), req.deviceFingerprint());
        WorkflowResponse wr = engine.resume(ctx, req.flowToken(), req);
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