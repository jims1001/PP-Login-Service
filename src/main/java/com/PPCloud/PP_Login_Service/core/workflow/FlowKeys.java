package com.PPCloud.PP_Login_Service.core.workflow;

/**
 * FlowKeys 定义 WorkflowState.bag 中使用的统一 key。
 *
 * 说明：
 * - 所有 key 都会随 workflow state 一起进入 stateToken（对客户端可见，但不可篡改）。
 * - 只应存放“已确认的流程事实”，不要放敏感数据或临时变量。
 */
public final class FlowKeys {
    private FlowKeys() {}

    /** 标识类型：EMAIL / PHONE / USERNAME */
    public static final String IDENTIFIER_TYPE = "idf.type";

    /** 标准化后的标识值（如 lower-case email / 去格式化手机号） */
    public static final String IDENTIFIER_NORM = "idf.norm";

    /** 本次流程使用的 identifier token） */
    public static final String IDENTIFIER_TOKEN       = "idf.token";

    /** 当前流程关联的用户 ID */
    public static final String USER_ID = "user.id";

    /** 是否已通过认证（用于后续步骤判断） */
    public static final String AUTH_OK = "auth.ok";

    /** 当前流程使用的 OTP challengeId */
    public static final String OTP_CHALLENGE_ID = "otp.challengeId";

    /** 下一段要切换到的 workflowId（HALT 时一次性使用） */
    public static final String NEXT_WORKFLOW_ID = "wf.next";

    // 授权方法
    public static final String AUTH_METHOD = "auth.method";


    /** OTP 是否已成功校验通过 */
    public static final String OTP_VERIFIED = "otp.verified";

    /** 流程最终对外返回的数据（仅 DONE 阶段使用） */
    public static final String RESULT = "result";

    /** 是否设置了密码（注册时可选） */
    public static final String PASSWORD_ENABLED = "pwd.enabled";

    /** 重置密码 actionToken 的 payload（consume 成功后写入，用于后续取 userId/identifier） */
    public static final String ACTION_TOKEN_PAYLOAD = "at.payload";

    /** actionToken 里的 userId（如果 payload 存了 userId，就提取到这里，后续步骤直接用） */
    public static final String ACTION_TOKEN_USER_ID = "at.userId";
}
