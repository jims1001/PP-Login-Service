package com.PPCloud.PP_Login_Service.model.user;

import com.PPCloud.PP_Login_Service.core.workflow.WorkflowContext;
import com.PPCloud.PP_Login_Service.model.BaseModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * IamAuthAudit
 *
 * 所有认证相关行为的审计日志。
 *
 * ⚠️ 永远只增不改
 */
@Document(collection = "iam_auth_audit")
@CompoundIndex(
        name = "idx_audit_user_time",
        def = "{'tenantId':1,'userId':1,'createdAt':-1}"
)
@Setter
@Getter
public class IamAuthAudit extends BaseModel {

    /**
     * 用户 ID（失败场景可能为空）
     */
    private String userId;

    private String tenantId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 失败/拒绝原因码
     */
    private String reasonCode;

    /**
     * 客户端 IP
     */
    private String ip;

    /**
     * User-Agent
     */
    private String ua;

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * AppClient / 业务系统标识
     */
    private String clientId;

    /**
     * 扩展元数据
     */
    private Object meta;


    public static IamAuthAudit simple(WorkflowContext ctx, String userId, String eventType, String reasonCode) {
        IamAuthAudit a = new IamAuthAudit();
        a.tenantId = ctx.tenantId;
        a.userId = userId;
        a.eventType = eventType;
        a.reasonCode = reasonCode;
        a.ip = ctx.ip;
        a.ua = ctx.ua;
        a.clientId = ctx.clientId;
        a.setCreatedAt( ctx.now);
        return a;
    }
}