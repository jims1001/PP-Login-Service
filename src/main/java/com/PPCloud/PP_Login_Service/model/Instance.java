package com.PPCloud.PP_Login_Service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter @Setter
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@Document("instance")
@CompoundIndexes({
        @CompoundIndex(name = "idx_tenant_code", def = "{ 'tenantId': 1, 'code': 1 }", unique = true),
        @CompoundIndex(name = "idx_issuer", def = "{ 'issuer': 1 }", unique = true)
})
public class Instance extends BaseModel {

    @Indexed
    private String tenantId;

    private String code;     // instanceCode
    private String name;

    /** OIDC Issuer */
    private String issuer;

    /** 物理部署信息 */
    private String region;
    private String cluster;
    private String dataPlaneRef;
}