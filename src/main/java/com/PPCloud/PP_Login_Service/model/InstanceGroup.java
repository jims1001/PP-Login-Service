package com.PPCloud.PP_Login_Service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter @Setter
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
@Document("pp_instance_group")
@CompoundIndex(
        name = "idx_tenant_name",
        def = "{ 'tenantId': 1, 'name': 1 }",
        unique = true
)
public class InstanceGroup extends BaseModel {

    @Indexed
    private String tenantId;

    private String name;

    private List<Member> members;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class Member {
        private String instanceId;
        private Integer priority;
    }
}