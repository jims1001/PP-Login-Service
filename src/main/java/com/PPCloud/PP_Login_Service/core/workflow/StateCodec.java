package com.PPCloud.PP_Login_Service.core.workflow;


/**
 * StateCodec：将 WorkflowState 编码/解码为 token
 *
 * 生产建议：
 * - token 加签名（HMAC/EdDSA）防篡改
 * - token 加过期时间（可放入 bag 或封装额外字段）
 */
public interface StateCodec {
    String encode(WorkflowState state);
    WorkflowState decode(String token);
}
