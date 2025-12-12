package com.PPCloud.PP_Login_Service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter @Setter
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@Document("pp_client_branding")
@CompoundIndex(name = "idx_client", def = "{ 'clientId': 1 }", unique = true)
public class ClientBranding extends BaseModel {

    @Indexed(unique = true)
    private String clientId;

    private String loginPageTemplateId;
    private Map<String, Object> theme;
    private String customCss;
}