package com.PPCloud.PP_Login_Service.Repository;

import com.PPCloud.PP_Login_Service.model.user.IamUserPassword;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IamUserPasswordRepository extends MongoRepository<IamUserPassword, String> {

    Optional<IamUserPassword> findByTenantIdAndUserId(String tenantId, String userId);

    boolean existsByTenantIdAndUserId(String tenantId, String userId);
}