package com.PPCloud.PP_Login_Service.model.user;

import com.PPCloud.PP_Login_Service.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * IamUserFactor
 *
 * 所有 MFA / 强认证因子的统一父表。
 *
 * 设计思想：
 * - 父表存「通用元信息」
 * - 子表存「因子专属数据」
 * - 新增因子类型不影响现有结构
 */
@Document(collection = "iam_user_factor")
@CompoundIndex(
        name = "idx_user_factor",
        def = "{'tenantId':1,'userId':1,'type':1,'status':1}"
)
@Setter
@Getter
public class IamUserFactor extends BaseModel {


    private Boolean enabled = true;

    private String tenantId;
    /**
     * 所属用户
     */
    private String userId;

    /**
     * 因子类型
     *
     * TOTP | PASSKEY | RECOVERY_CODE
     */
    private String type;

    /**
     * 用户自定义名称
     *
     * 例如：我的手机 / 公司电脑
     */
    private String name;

//    /**
//     * 因子状态
//     *
//     * ENABLED | DISABLED
//     */
//    private String status;

    /**
     * 最近一次使用时间
     */
    private long lastUsedAt;
}