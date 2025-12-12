package com.PPCloud.PP_Login_Service.Repository;

import com.PPCloud.PP_Login_Service.model.user.IamActionToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IamActionTokenRepository extends MongoRepository<IamActionToken, String> {

    Optional<IamActionToken> findByTenantIdAndId(String tenantId, String tokenId);

    Optional<IamActionToken> findByTenantIdAndTypeAndTokenHashAndUsedAtIsNull(
            String tenantId, String type, String tokenHash
    );
}