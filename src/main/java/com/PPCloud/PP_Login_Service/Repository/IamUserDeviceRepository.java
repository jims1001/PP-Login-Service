package com.PPCloud.PP_Login_Service.Repository;

import com.PPCloud.PP_Login_Service.model.user.IamUserDevice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IamUserDeviceRepository extends MongoRepository<IamUserDevice, String> {

    Optional<IamUserDevice> findByTenantIdAndUserIdAndDeviceFingerprint(
            String tenantId, String userId, String deviceFingerprint
    );

    List<IamUserDevice> findByTenantIdAndUserIdOrderByLastSeenAtDesc(String tenantId, String userId);
}