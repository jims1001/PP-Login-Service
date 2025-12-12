package com.PPCloud.PP_Login_Service.model;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter @Setter
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@Document("pp_tenant")
@CompoundIndex(name = "idx_tenant_code", def = "{ 'code': 1 }", unique = true)
public class Tenant extends BaseModel {

    @Indexed(unique = true)
    private String code;   // tenantCode / merchantCode

    private String name;
}