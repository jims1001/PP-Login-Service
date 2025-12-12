package com.PPCloud.PP_Login_Service.Repository;

import com.PPCloud.PP_Login_Service.model.user.IamAuthAudit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface IamAuthAuditRepository extends MongoRepository<IamAuthAudit, String> {

    List<IamAuthAudit> findByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, String userId);

    List<IamAuthAudit> findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String tenantId, Instant from, Instant to
    );

    List<IamAuthAudit> findByTenantIdAndEventTypeOrderByCreatedAtDesc(String tenantId, String eventType);
}