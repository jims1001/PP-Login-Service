package com.PPCloud.PP_Login_Service.Repository;

import com.PPCloud.PP_Login_Service.model.user.IamUserFactor;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IamUserFactorRepository extends MongoRepository<IamUserFactor, String> {

    List<IamUserFactor> findByTenantIdAndUserId(String tenantId, String userId);

    List<IamUserFactor> findByTenantIdAndUserIdAndStatus(String tenantId, String userId, String status);

    List<IamUserFactor> findByTenantIdAndUserIdAndTypeAndStatus(String tenantId, String userId, String type, String status);

    Optional<IamUserFactor> findByTenantIdAndId(String tenantId, String factorId);

    List<IamUserFactor> findByTenantIdAndUserIdAndEnabledTrue(String tenantId, String userId);
}