package com.PPCloud.PP_Login_Service.model.user;

import com.PPCloud.PP_Login_Service.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


/**
 * IamUserPassword
 *
 * 用户密码凭证表。
 *
 * 设计原则：
 * - 一用户一条
 * - 只存 hash，不存明文
 * - 锁定 / 失败次数与主体解耦
 */
@Document(collection = "iam_user_password")

@Getter
@Setter
public class IamUserPassword extends BaseModel {

    private String tenantId;
    /**
     * 用户 ID
     *
     * unique，保证一人一份密码记录
     */
    @Indexed(unique = true)
    private String userId;

    /**
     * 密码 hash
     *
     * 推荐：argon2id
     */
    private String passwordHash;

    /**
     * hash 算法标识
     */
    private String algo;

    /**
     * 最近一次修改时间
     */
    private long changedAt;

    /**
     * 是否要求下次登录强制修改密码
     */
    private boolean mustChange;

    /**
     * 连续失败次数
     */
    private int failedCount;

    /**
     * 最近一次失败时间
     */
    private long lastFailedAt;

    /**
     * 锁定到期时间
     *
     * null = 未锁定
     */
    private long lockedUntil;
}
