package com.PPCloud.PP_Login_Service.flow;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultWorkflowResolver implements WorkflowResolver {

    private final Map<String, String> map = new ConcurrentHashMap<>();

    public DefaultWorkflowResolver() {
        // 默认 client（通配）
        put("10000", "*", FlowKind.LOGIN_IDENTIFY, "WF_LOGIN_IDENTIFY_V1");
        put("10000", "*", FlowKind.LOGIN_PASSWORD, "WF_LOGIN_PASSWORD_V1");

        put("10000","pp-login-web", FlowKind.REGISTER_START, "WF_REGISTER_START_V1");
        put("10000","pp-login-web", FlowKind.REGISTER_VERIFY, "WF_REGISTER_VERIFY_V1");

        // 某个 client 走不同 identify（例如更强风控/额外 step）
        put("10000", "pp-login-web", FlowKind.LOGIN_IDENTIFY, "WF_LOGIN_IDENTIFY_V1");
        put("10000", "pp-login-web", FlowKind.LOGIN_PASSWORD, "WF_LOGIN_PASSWORD_V2");

        // 某个 client 不允许密码，走 OTP / 或强制 MFA（示例）
        put("10000", "partner-portal", FlowKind.LOGIN_PASSWORD, "WF_LOGIN_PASSWORD_PARTNER_V1");
    }

    @Override
    public String resolveWorkflowId(String tenantId, String clientId, FlowKind kind) {
        String k1 = key(tenantId, clientId, kind);
        if (map.containsKey(k1)) return map.get(k1);

        String k2 = key(tenantId, "*", kind);
        if (map.containsKey(k2)) return map.get(k2);

        throw new IllegalArgumentException("No workflow mapping for " + tenantId + "," + clientId + "," + kind);
    }

    private void put(String tid, String cid, FlowKind kind, String wf) {
        map.put(key(tid, cid, kind), wf);
    }

    private String key(String tid, String cid, FlowKind kind) {
        return tid + "|" + cid + "|" + kind.name();
    }
}