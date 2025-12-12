package com.PPCloud.PP_Login_Service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter @Setter
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@Document("pp_domain_binding")
@CompoundIndex(name = "idx_domain", def = "{ 'domain': 1 }", unique = true)
public class DomainBinding extends BaseModel {

    @Indexed(unique = true)
    private String domain;

    @Indexed
    private String tenantId;

    private BindingType bindingType;
    private String bindingId;

    private String defaultClientId;
    private TlsMode tlsMode;

    public enum BindingType {
        INSTANCE,
        GROUP,
        CLIENT
    }

    public enum TlsMode {
        MANAGED,
        UPLOADED,
        EXTERNAL
    }
}