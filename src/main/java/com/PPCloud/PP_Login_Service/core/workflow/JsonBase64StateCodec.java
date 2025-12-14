package com.PPCloud.PP_Login_Service.core.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * JsonBase64StateCodec：简单实现（仅 Base64(JSON)）
 * TODO：生产必须加签名，防止用户篡改 bag/currentStepIndex
 */

@Component("jsonBase64StateCodec")
public class JsonBase64StateCodec implements StateCodec {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String encode(WorkflowState state) {
        try {
            byte[] json = om.writeValueAsBytes(state);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WorkflowState decode(String token) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(token);
            return om.readValue(json, WorkflowState.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}