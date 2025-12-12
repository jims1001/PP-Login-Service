package com.PPCloud.PP_Login_Service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Getter @Setter
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
public abstract class BaseModel {

    @Id
    private String id;

    @CreatedDate
    @Field("created_at")
    private long createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private long updatedAt;

    @Version
    @Field("ver")
    private Long version;

    @Field("status")
    private Status status;

    public enum Status { ACTIVE, SUSPENDED, DELETED }
}
