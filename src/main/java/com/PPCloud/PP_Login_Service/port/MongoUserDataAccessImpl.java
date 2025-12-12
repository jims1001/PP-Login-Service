package com.PPCloud.PP_Login_Service.port;

import com.PPCloud.PP_Login_Service.Repository.*;
import com.PPCloud.PP_Login_Service.model.user.*;
import com.PPCloud.PP_Login_Service.port.otp.ActionTokenConsumeResult;
import com.PPCloud.PP_Login_Service.port.otp.OtpVerifyResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * MongoUserDataAccessImpl
 *
 * - 普通 CRUD：直接用 Spring Data Repository（你已经有了）
 * - 强一致/原子操作：用 MongoTemplate.findAndModify / updateFirst
 *
 * 这样 workflow/steps 永远只依赖 UserDataAccess（不直接依赖 Mongo）
 */

@Service
public class MongoUserDataAccessImpl implements UserDataAccess {

    private final IamUserRepository iamUserRepository;
    private final IamUserIdentifierRepository iamUserIdentifierRepository;
    private final IamUserPasswordRepository iamUserPasswordRepository;
    private final IamUserFactorRepository iamUserFactorRepository;
    private final IamUserDeviceRepository iamUserDeviceRepository;
    private final IamOtpChallengeRepository iamOtpChallengeRepository;
    private final IamActionTokenRepository iamActionTokenRepository;

    private final MongoTemplate mongoTemplate;

    public MongoUserDataAccessImpl(
            IamUserRepository iamUserRepository,
            IamUserIdentifierRepository iamUserIdentifierRepository,
            IamUserPasswordRepository iamUserPasswordRepository,
            IamUserFactorRepository iamUserFactorRepository,
            IamUserDeviceRepository iamUserDeviceRepository,
            IamOtpChallengeRepository iamOtpChallengeRepository,
            IamActionTokenRepository iamActionTokenRepository,
            MongoTemplate mongoTemplate
    ) {
        this.iamUserRepository = iamUserRepository;
        this.iamUserIdentifierRepository = iamUserIdentifierRepository;
        this.iamUserPasswordRepository = iamUserPasswordRepository;
        this.iamUserFactorRepository = iamUserFactorRepository;
        this.iamUserDeviceRepository = iamUserDeviceRepository;
        this.iamOtpChallengeRepository = iamOtpChallengeRepository;
        this.iamActionTokenRepository = iamActionTokenRepository;
        this.mongoTemplate = mongoTemplate;
    }

    // ---------------- user / identifier ----------------

    @Override
    public Optional<IamUserIdentifier> findIdentifier(String tenantId, String type, String identifierNorm) {
        // ✅ 推荐：在 Repository 里定义 findByTenantIdAndTypeAndIdentifier(...)
        return iamUserIdentifierRepository.findByTenantIdAndTypeAndIdentifier(tenantId, type, identifierNorm);
    }

    @Override
    public List<IamUserIdentifier> listIdentifiers(String tenantId, String userId) {
        return iamUserIdentifierRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    @Override
    public Optional<IamUser> findUser(String tenantId, String userId) {
        // 这里看你 IamUser 是否 tenantId 分片；如果 _id 全局唯一也可只 findById
        return iamUserRepository.findByTenantIdAndId(tenantId, userId);
    }

    @Override
    public String createUser(IamUser user) {
        return iamUserRepository.save(user).getId();
    }

    @Override
    public void saveIdentifier(IamUserIdentifier identifier) {
        iamUserIdentifierRepository.save(identifier);
    }

    @Override
    public void markIdentifierVerified(String tenantId, String type, String identifierNorm, long verifiedAt) {
        // 你也可以做成 Repository update；这里用 MongoTemplate 直接 update
        Query q = Query.query(Criteria.where("tenantId").is(tenantId)
                .and("type").is(type)
                .and("identifier").is(identifierNorm));

        Update u = new Update()
                .set("verifiedAt", verifiedAt)
                .set("updatedAt", verifiedAt);

        mongoTemplate.updateFirst(q, u, IamUserIdentifier.class);
    }


    // ---------------- password ----------------

    @Override
    public Optional<IamUserPassword> findPassword(String tenantId, String userId) {
        return iamUserPasswordRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    @Override
    public void savePassword(IamUserPassword password) {
        iamUserPasswordRepository.save(password);
    }

    @Override
    public void bumpPasswordFail(String tenantId, String userId, long now) {
        Query q = Query.query(Criteria.where("tenantId").is(tenantId).and("userId").is(userId));
        Update u = new Update().inc("failCount", 1).set("lastFailAt", now);
        mongoTemplate.updateFirst(q, u, IamUserPassword.class);
    }

    @Override
    public void clearPasswordFail(String tenantId, String userId, long now) {
        Query q = Query.query(Criteria.where("tenantId").is(tenantId).and("userId").is(userId));
        Update u = new Update().set("failCount", 0).unset("lockedUntil").set("lastSuccessAt", now);
        mongoTemplate.updateFirst(q, u, IamUserPassword.class);
    }




    // ---------------- factor / device ----------------

    @Override
    public List<IamUserFactor> listEnabledFactors(String tenantId, String userId) {
        return iamUserFactorRepository.findByTenantIdAndUserIdAndEnabledTrue(tenantId, userId);
    }

    @Override
    public Optional<IamUserDevice> findDevice(String tenantId, String userId, String deviceFingerprint) {
        return iamUserDeviceRepository.findByTenantIdAndUserIdAndDeviceFingerprint(tenantId, userId, deviceFingerprint);
    }

    @Override
    public void upsertDeviceSeen(String tenantId, String userId, String deviceFingerprint, String ua, String platform, long seenAt) {
        Query q = Query.query(Criteria.where("tenantId").is(tenantId)
                .and("userId").is(userId)
                .and("deviceFingerprint").is(deviceFingerprint));

        Update u = new Update()
                .setOnInsert("tenantId", tenantId)
                .setOnInsert("userId", userId)
                .setOnInsert("deviceFingerprint", deviceFingerprint)
                .set("ua", ua)
                .set("platform", platform)
                .set("lastSeenAt", seenAt)
                .inc("seenCount", 1);

        mongoTemplate.upsert(q, u, IamUserDevice.class);
    }



    // ---------------- otp (原子) ----------------

    @Override
    public String createOtp(IamOtpChallenge otp) {
        return iamOtpChallengeRepository.save(otp).getId();
    }

    /**
     * 原子校验 OTP：
     * - 未过期 expiresAt > now
     * - 未通过 passedAt == null
     * - attempts < maxAttempts
     * - codeHash 匹配
     * 成功：写 passedAt
     * 失败：attempts++（也尽量原子）
     */
    @Override
    public OtpVerifyResult verifyOtpAtomically(String tenantId, String challengeId, String codeHash, long now) {

        // 1) 尝试“成功通过”
        Query passQ = Query.query(Criteria.where("tenantId").is(tenantId)
                .and("_id").is(challengeId)
                .and("expiresAt").gt(now)
                .and("passedAt").is(null)
                .and("codeHash").is(codeHash)
        );

        // attempts < maxAttempts：如果你的版本不支持字段比较，就先读出来再判断（见下方 fallback）
        // 这里给一个保守写法：直接用 attempts < 5（你 maxAttempts 固定 5 的话）
        passQ.addCriteria(Criteria.where("attempts").lt(5));

        Update passU = new Update()
                .set("passedAt", now)
                .set("updatedAt", now);

        IamOtpChallenge passed = mongoTemplate.findAndModify(
                passQ, passU,
                FindAndModifyOptions.options().returnNew(true),
                IamOtpChallenge.class
        );

        if (passed != null) return new OtpVerifyResult(true, "OK");

        // 2) 失败：attempts++（尽量也原子）
        Query incQ = Query.query(Criteria.where("tenantId").is(tenantId)
                .and("_id").is(challengeId)
                .and("expiresAt").gt(now)
                .and("passedAt").is(null)
                .and("attempts").lt(5)
        );

        Update incU = new Update()
                .inc("attempts", 1)
                .set("lastAttemptAt", now)
                .set("updatedAt", now);

        mongoTemplate.updateFirst(incQ, incU, IamOtpChallenge.class);

        return new OtpVerifyResult(false, "INVALID_CODE_OR_EXPIRED");
    }



    // ---------------- action token (原子) ----------------

    @Override
    public String createActionToken(IamActionToken token) {
        return iamActionTokenRepository.save(token).getId();
    }

    /**
     * 原子消费 ActionToken（一次性）：
     * - expiresAt > now
     * - usedAt == null
     * - type + tokenHash 匹配
     * 成功：写 usedAt，并返回 payload
     */
    @Override
    public ActionTokenConsumeResult consumeActionTokenAtomically(String tenantId, String type, String tokenHash, long now) {
        Query q = Query.query(Criteria.where("tenantId").is(tenantId)
                .and("type").is(type)
                .and("tokenHash").is(tokenHash)
                .and("expiresAt").gt(now)
                .and("usedAt").is(null));

        Update u = new Update().set("usedAt", now).set("updatedAt", now);

        IamActionToken used = mongoTemplate.findAndModify(
                q, u,
                FindAndModifyOptions.options().returnNew(true),
                IamActionToken.class
        );

        if (used == null) return new ActionTokenConsumeResult(false, "TOKEN_INVALID_OR_USED", null);
        return new ActionTokenConsumeResult(true, "OK", used.getPayload());
    }


}