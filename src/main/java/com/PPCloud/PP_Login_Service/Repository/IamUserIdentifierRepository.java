package com.PPCloud.PP_Login_Service.Repository;



import com.PPCloud.PP_Login_Service.model.user.IamUserIdentifier;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IamUserIdentifierRepository extends MongoRepository<IamUserIdentifier, String> {

    Optional<IamUserIdentifier> findByTenantIdAndTypeAndIdentifier(String tenantId, String type, String identifier);

    List<IamUserIdentifier> findByTenantIdAndUserId(String tenantId, String userId);

    Optional<IamUserIdentifier> findByTenantIdAndUserIdAndTypeAndPrimaryTrue(
            String tenantId, String userId, String type
    );

    List<IamUserIdentifier> findByTenantIdAndUserIdAndType(String tenantId, String userId, String type);
}