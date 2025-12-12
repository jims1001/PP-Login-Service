package com.PPCloud.PP_Login_Service.Repository;

import com.PPCloud.PP_Login_Service.model.user.IamOtpChallenge;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IamOtpChallengeRepository extends MongoRepository<IamOtpChallenge, String> {

    Optional<IamOtpChallenge> findByTenantIdAndId(String tenantId, String challengeId);

    List<IamOtpChallenge> findByTenantIdAndChannelAndTargetOrderByCreatedAtDesc(
            String tenantId, String channel, String target
    );

    List<IamOtpChallenge> findByTenantIdAndExpiresAtAfterAndPassedAtIsNull(String tenantId, Instant now);
}
