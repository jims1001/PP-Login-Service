package com.PPCloud.PP_Login_Service.flow;

/**
 * FlowKind 表示「身份相关业务流程的阶段类型」。
 *
 * 说明：
 * - FlowKind 是抽象阶段（Stage / Kind），不是具体的 workflowId。
 * - 每个 FlowKind 可由不同 client 映射到不同的 workflow 实现（如 WF_xxx_V1 / V2）。
 * - 一个完整业务流程通常由多个 FlowKind 顺序组成。
 *
 * 生命周期示例：
 * - Login    : LOGIN_IDENTIFY  -> LOGIN_PASSWORD
 * - Register : REGISTER_START  -> REGISTER_VERIFY -> REGISTER_COMMIT
 * - Reset    : RESET_START     -> RESET_VERIFY    -> RESET_COMMIT
 */
public enum FlowKind {

    /**
     * 登录第一阶段：账号识别阶段。
     *
     * 职责：
     * - 标准化 identifier（email / phone / username）
     * - 查找用户与账号能力
     * - 决定下一步登录方式（PASSWORD / OTP / MFA / REJECT）
     *
     * 结果：
     * - HALT，返回 nextAction 给前端
     */
    LOGIN_IDENTIFY,

    /**
     * 登录凭证验证阶段（等价于登录提交 / commit）。
     *
     * 职责：
     * - 校验密码 / OTP / Passkey 等凭证
     * - 进行风控 / 设备校验
     * - 登录成功后签发 access token / refresh token
     *
     * 结果：
     * - DONE（登录完成）
     */
    LOGIN_PASSWORD,      // == LOGIN_COMMIT


    /**
     * 注册起始阶段。
     *
     * 职责：
     * - 标准化 identifier
     * - 创建用户与标识（未验证）
     * - 发送注册验证码（OTP / Email / SMS）
     *
     * 结果：
     * - HALT，等待用户完成验证
     */
    REGISTER_START,

    /**
     * 注册验证阶段。
     *
     * 职责：
     * - 校验注册验证码
     * - 标记 identifier 为 verified
     *
     * 结果：
     * - HALT 或进入注册提交阶段
     */
    REGISTER_VERIFY,

    /**
     * 注册提交阶段（注册完成）。
     *
     * 职责：
     * - 设置初始凭证（密码 / passkey）
     * - 完成用户初始化
     * - 可选：自动登录并签发 token
     *
     * 结果：
     * - DONE（注册完成）
     */
    REGISTER_COMMIT,


    /**
     * 重置密码起始阶段。
     *
     * 职责：
     * - 标准化 identifier
     * - 发送重置验证码
     *
     * 结果：
     * - HALT，等待验证码
     */
    RESET_START,

    /**
     * 重置验证阶段。
     *
     * 职责：
     * - 校验重置验证码
     * - 签发一次性 reset action token
     *
     * 结果：
     * - HALT，等待提交新密码
     */
    RESET_VERIFY,

    /**
     * 重置提交阶段。
     *
     * 职责：
     * - 校验并消费 reset action token
     * - 设置新密码
     *
     * 结果：
     * - DONE（密码重置完成）
     */
    RESET_COMMIT
}