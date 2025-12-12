package com.PPCloud.PP_Login_Service.model.user;

import com.PPCloud.PP_Login_Service.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;
/**
 * IamUserDevice
 *
 * 用户设备记录，用于：
 * - 新设备识别
 * - Step-Up MFA
 * - 风控分析
 */
@Document(collection = "iam_user_device")
@CompoundIndex(
        name = "uniq_user_device",
        def = "{'tenantId':1,'userId':1,'deviceFingerprint':1}",
        unique = true
)
@Setter
@Getter
public class IamUserDevice extends BaseModel {

    private String tenantId;
    /**
     * 所属用户
     */
    private String userId;

    /**
     * 设备指纹 hash
     */
    private String deviceFingerprint;

    /**
     * User-Agent
     */
    private String ua;

    /**
     * 平台信息
     */
    private String platform;

    /**
     * 首次出现时间
     */
    private long firstSeenAt;

    /**
     * 最近一次出现时间
     */
    private long lastSeenAt;

    /**
     * 标记为可信设备时间
     */
    private long trustedAt;

    /**
     * 吊销时间
     */
    private long revokedAt;
}