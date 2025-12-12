package com.PPCloud.PP_Login_Service.flow;

import com.PPCloud.PP_Login_Service.api.dto.*;

public interface AuthFlowFacade {

    FlowResult register(RegisterReq req);
    FlowResult registerVerify(RegisterVerifyReq req);

    FlowResult loginIdentify(LoginIdentifyReq req);
    FlowResult loginPassword(LoginPasswordReq req);

    FlowResult resetStart(ResetStartReq req);
    FlowResult resetVerify(ResetVerifyReq req);
    FlowResult resetCommit(ResetCommitReq req);
}