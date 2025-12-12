package com.PPCloud.PP_Login_Service.model.user;

import com.PPCloud.PP_Login_Service.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * IamUser
 *
 * 用户“身份主体”表。
 *
 * ⚠️ 重要设计原则：
 * - 这里只表示“这个人存在”
 * - 不包含任何登录方式、凭证、权限、组织信息
 * - 是整个 IAM 系统中最稳定的一张表
 */
@Document(collection = "iam_user")
@Setter
@Getter
public class IamUser extends BaseModel {


    private String tenantId;

    private String Id;
    /**
     * 用户状态
     *
     * ACTIVE    : 正常可用
     * DISABLED  : 管理员禁用
     * LOCKED    : 安全策略锁定（如密码失败）
     * DELETED   : 逻辑删除
     *
     * 状态机由业务层控制，数据库只存结果
     */

    private String UserStatus;

    /**
     * 显示名称
     *
     * 用于前端展示，不参与任何认证逻辑
     */
    private String displayName;

    /**
     * 头像地址
     */
    private String avatarUrl;

    /**
     * 用户语言偏好
     */
    private String locale;

    /**
     * 用户时区
     */
    private String timezone;

    /**
     * 禁用时间
     *
     * 用于审计 / 自动解禁
     */
    private long disabledAt;

    /**
     * 逻辑删除时间
     *
     * 真实删除通常由离线任务完成
     */
    private long deletedAt;
}