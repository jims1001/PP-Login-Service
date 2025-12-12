package com.PPCloud.PP_Login_Service.Repository;

import com.PPCloud.PP_Login_Service.model.user.IamUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IamUserRepository extends MongoRepository<IamUser, String> {

    Optional<IamUser> findByTenantIdAndId(String tenantId, String userId);

    List<IamUser> findByTenantIdAndStatus(String tenantId, String status);
}