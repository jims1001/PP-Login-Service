package com.PPCloud.PP_Login_Service.model.user;

import com.PPCloud.PP_Login_Service.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * IamOtpChallenge
 *
 * 短信 / 邮箱 OTP 挑战记录。
 *
 * 用于：
 * - 登录
 * - 注册验证
 * - 找回密码
 *
 * ⚠️ 强烈建议使用 TTL index 自动清理
 */
@Document(collection = "iam_otp_challenge")

@Setter
@Getter
public class IamOtpChallenge extends BaseModel {

    private String tenantId;
    /**
     * 使用场景
     */
    private String purpose;

    /**
     * 通道类型
     */
    private String channel;

    /**
     * 目标地址
     *
     * email / phone（规范化）
     */
    private String target;

    /**
     * 验证码 hash
     */
    private String codeHash;

    /**
     * 登录事务 ID（可选）
     */
    private String txId;

    /**
     * 已尝试次数
     */
    private int attempts;

    /**
     * 最大允许次数
     */
    private int maxAttempts;


    private long expiresAt;

    /**
     * 验证成功时间
     */
    private long passedAt;
}