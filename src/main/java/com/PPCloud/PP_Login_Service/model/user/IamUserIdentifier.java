package com.PPCloud.PP_Login_Service.model.user;

import com.PPCloud.PP_Login_Service.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * IamUserIdentifier
 *
 * 用户的「可登录标识」表。
 *
 * 这是通用用户系统中最关键的模型之一。
 *
 * 设计目的：
 * - 支持 username / email / phone 任意组合
 * - 支持一个用户多个标识
 * - 支持标识变更、验证、解绑
 */
@Document(collection = "iam_user_identifier")
@CompoundIndexes({
        /**
         * 同租户下：
         * 同一类型 + 同一标识只能属于一个用户
         *
         * 例如：
         * tenantA + EMAIL + test@example.com → 唯一
         */
        @CompoundIndex(
                name = "uniq_identifier",
                def = "{'tenantId':1,'type':1,'identifier':1}",
                unique = true
        ),

        /**
         * 用户维度的查询索引
         */
        @CompoundIndex(
                name = "idx_user_type",
                def = "{'tenantId':1,'userId':1,'type':1}"
        )
})

@Setter
@Getter
public class IamUserIdentifier extends BaseModel {

    private String tenantId;
    /**
     * 所属用户
     */
    private String userId;

    /**
     * 标识类型
     *
     * USERNAME | EMAIL | PHONE
     *
     * ⚠️ 不要用 boolean 字段，必须可扩展
     */
    private String type;

    /**
     * 规范化后的标识值
     *
     * - email：lowercase
     * - phone：E.164
     * - username：lowercase / normalize
     *
     * ⚠️ 唯一索引基于该字段
     */
    private String identifier;

    /**
     * 是否主标识
     *
     * 例如：主邮箱 / 主手机号
     */
    private boolean primary;

    /**
     * 标识是否已验证
     *
     * 例如：
     * - 邮箱点击验证
     * - 手机短信验证
     */
    private long verifiedAt;
}