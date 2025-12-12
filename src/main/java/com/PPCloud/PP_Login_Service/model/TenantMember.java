package com.PPCloud.PP_Login_Service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter @Setter
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@Document("pp_tenant_member")
@CompoundIndex(
        name = "idx_tenant_user",
        def = "{ 'tenantId': 1, 'userId': 1 }",
        unique = true
)
public class TenantMember extends BaseModel {

    @Indexed
    private String tenantId;

    @Indexed
    private String userId;

    private Role role;

    public enum Role {
        OWNER,
        ADMIN,
        AUDITOR,
        OPS
    }
}
