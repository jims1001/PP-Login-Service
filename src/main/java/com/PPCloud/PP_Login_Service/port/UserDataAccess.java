package com.PPCloud.PP_Login_Service.port;

import com.PPCloud.PP_Login_Service.model.user.*;
import com.PPCloud.PP_Login_Service.port.otp.ActionTokenConsumeResult;
import com.PPCloud.PP_Login_Service.port.otp.OtpVerifyResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * UserDataAccess：认证工作流的“数据访问门面”
 *
 * ✅ 只围绕你现有 8 张表：
 * - IamUser / IamUserIdentifier / IamUserPassword / IamUserFactor / IamUserDevice
 * - IamOtpChallenge / IamActionToken / IamAuthAudit（审计走 AuditSink）
 *
 * 未来换库：只换实现，不动 workflow steps。
 */
public interface UserDataAccess {

    // --- user / identifier ---
    Optional<IamUserIdentifier> findIdentifier(String tenantId, String type, String identifierNorm);
    List<IamUserIdentifier> listIdentifiers(String tenantId, String userId);
    Optional<IamUser> findUser(String tenantId, String userId);

    String createUser(IamUser user);
    void saveIdentifier(IamUserIdentifier identifier);
    void markIdentifierVerified(String tenantId, String type, String identifierNorm, long verifiedAt);


    // --- password ---
    Optional<IamUserPassword> findPassword(String tenantId, String userId);
    void savePassword(IamUserPassword password);

    // 可选：失败计数/锁定（你以后加字段再实现）
    default void bumpPasswordFail(String tenantId, String userId, long now) {}




    void clearPasswordFail(String tenantId, String userId, long now);

    // --- factor / device ---
    List<IamUserFactor> listEnabledFactors(String tenantId, String userId);
    Optional<IamUserDevice> findDevice(String tenantId, String userId, String deviceFingerprint);
    void upsertDeviceSeen(String tenantId, String userId, String deviceFingerprint, String ua, String platform, long seenAt);


    // --- otp ---
    String createOtp(IamOtpChallenge otp);
    OtpVerifyResult verifyOtpAtomically(String tenantId, String challengeId, String codeHash, long now);



    // --- action token ---
    String createActionToken(IamActionToken token);
    ActionTokenConsumeResult consumeActionTokenAtomically(String tenantId, String type, String tokenHash, long now);

}