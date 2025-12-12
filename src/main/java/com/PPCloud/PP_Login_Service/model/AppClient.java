package com.PPCloud.PP_Login_Service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Getter @Setter
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@Document("pp_app_client")
@CompoundIndex(name = "idx_client_id", def = "{ 'clientId': 1 }", unique = true)
public class AppClient extends BaseModel {

    @Indexed(unique = true)
    private String clientId;

    @Indexed
    private String tenantId;

    /** 二选一 */
    private String instanceId;
    private String groupId;

    private String name;

    private ClientType type;
    private String secretHash;

    private List<String> grantTypes;
    private List<String> redirectUris;
    private List<String> scopes;

    private String tokenEndpointAuthMethod;
    private String jwksUri;

    private Map<String, Object> ext;

    public enum ClientType {
        PUBLIC,
        CONFIDENTIAL
    }
}