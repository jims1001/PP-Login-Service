package com.PPCloud.PP_Login_Service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter @Setter
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@Document("pp_route_rule")
@CompoundIndex(
        name = "idx_tenant_priority",
        def = "{ 'tenantId': 1, 'priority': 1 }"
)
public class RouteRule extends BaseModel {

    @Indexed
    private String tenantId;

    private MatchType matchType;
    private String matchExpr;

    private TargetType targetType;
    private String targetId;

    private Integer priority;
    private Boolean enabled;

    public enum MatchType {
        DOMAIN,
        PATH,
        HEADER,
        QUERY
    }

    public enum TargetType {
        INSTANCE,
        GROUP,
        CLIENT
    }
}
