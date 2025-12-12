package com.PPCloud.PP_Login_Service.model.user;

import com.PPCloud.PP_Login_Service.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * IamActionToken
 *
 * 用于各种“需要一次性确认”的操作：
 * - 重置密码
 * - 验证邮箱
 * - 验证手机号
 * - 解锁账户
 */
@Document(collection = "iam_action_token")
@Setter
@Getter
public class IamActionToken extends BaseModel {

    /**
     * 关联用户（注册前可为空）
     */
    private String userId;

    private String tenantId;

    /**
     * 动作类型
     */
    private String type;

    /**
     * token hash
     */
    private String tokenHash;

    /**
     * 扩展数据
     *
     * 例如：待验证邮箱
     */
    private Object payload;

    /**
     * 过期时间（TTL）
     */
    private long expiresAt;

    /**
     * 已使用时间
     */
    private long usedAt;
}
