package com.PPCloud.PP_Login_Service.api.controllers;

import com.PPCloud.PP_Login_Service.api.dto.*;
import com.PPCloud.PP_Login_Service.flow.AuthFlowFacade;
import com.PPCloud.PP_Login_Service.flow.FlowResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对外接口仍然是“注册/登录/找回密码”
 * 内部实际执行：workflow start/resume
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthFlowFacade facade;

    public AuthController(AuthFlowFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/register")
    public FlowResult register(@RequestBody RegisterReq req) {
        // register/start：走 WF_REGISTER_START_V1
        return facade.register(req);
    }

    @PostMapping("/register/verify")
    public FlowResult registerVerify(@RequestBody RegisterVerifyReq req) {
        // register/verify：走 WF_REGISTER_VERIFY_V1（resume）
        return facade.registerVerify(req);
    }

    @PostMapping("/login/identify")
    public FlowResult loginIdentify(@RequestBody LoginIdentifyReq req) {
        // login identify：走 WF_LOGIN_IDENTIFY_V1
        return facade.loginIdentify(req);
    }

    @PostMapping("/login/password")
    public FlowResult loginPassword(@RequestBody LoginPasswordReq req) {
        // login password：resume WF_LOGIN_PASSWORD_V1
        return facade.loginPassword(req);
    }

    @PostMapping("/password/reset/start")
    public FlowResult resetStart(@RequestBody ResetStartReq req) {
        return facade.resetStart(req);
    }

    @PostMapping("/password/reset/verify")
    public FlowResult resetVerify(@RequestBody ResetVerifyReq req) {
        return facade.resetVerify(req);
    }

    @PostMapping("/password/reset/commit")
    public FlowResult resetCommit(@RequestBody ResetCommitReq req) {
        return facade.resetCommit(req);
    }
}