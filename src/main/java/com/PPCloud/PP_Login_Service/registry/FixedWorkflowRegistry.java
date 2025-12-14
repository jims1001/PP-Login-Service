package com.PPCloud.PP_Login_Service.registry;

import com.PPCloud.PP_Login_Service.core.workflow.*;
import com.PPCloud.PP_Login_Service.steps.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * FixedWorkflowRegistry：固定流程编排（第一阶段）
 *
 * 未来可配置：把 getDefinition(...) 改成从 DB 读 WorkflowDefinition
 */

@Service
public class FixedWorkflowRegistry implements WorkflowRegistry {

    private final StepFactory factory = new AuthStepFactory();

    @Override
    public WorkflowDefinition getDefinition(String workflowId) {
        return switch (workflowId) {

            // 注册：start（创建用户 + 发送验证码 → HALT）
            case "WF_REGISTER_START_V1" -> new WorkflowDefinition(
                    "WF_REGISTER_START_V1",
                    new WorkflowVersion(1, 0),
                    List.of(
                            new StepConfig("normalize", "NORMALIZE_IDENTIFIER", Map.of()),
                            new StepConfig("register",  "REGISTER_CREATE_USER",    Map.of()),
                            new StepConfig("sendOtp",   "OTP_SEND_REGISTER",      Map.of("ttlSeconds", 300))
                            // sendOtp 会 HALT
                    )
            );

            // 注册：verify（校验验证码 + 标记 verified → DONE）
            case "WF_REGISTER_VERIFY_V1" -> new WorkflowDefinition(
                    "WF_REGISTER_VERIFY_V1",
                    new WorkflowVersion(1, 0),
                    List.of(
                            new StepConfig("verifyOtp", "OTP_VERIFY_REGISTER", Map.of()),
                            new StepConfig("markV",     "IDENTIFIER_MARK_VERIFIED", Map.of())
                    )
            );

            // 登录：identify（查用户能力 → HALT）
            case "WF_LOGIN_IDENTIFY_V1" -> new WorkflowDefinition(
                    "WF_LOGIN_IDENTIFY_V1",
                    new WorkflowVersion(1, 0),
                    List.of(
                            new StepConfig("normalize", "NORMALIZE_IDENTIFIER", Map.of()),
                            new StepConfig("lookup",    "LOGIN_LOOKUP",         Map.of())
                            // lookup 会 HALT，告诉前端 nextAction=PWD/OTP/REJECT
                    )
            );

            // 登录：password（校验密码 → DONE 或 NEED_ACTION(step-up/mfa)）
            case "WF_LOGIN_PASSWORD_V1" -> new WorkflowDefinition(
                    "WF_LOGIN_PASSWORD_V1",
                    new WorkflowVersion(1, 0),
                    List.of(
                            new StepConfig("normalize", "NORMALIZE_IDENTIFIER", Map.of()),
                            new StepConfig("pwd",       "LOGIN_VERIFY_PASSWORD", Map.of()),
                            new StepConfig("device",    "DEVICE_UPSERT_SEEN", Map.of())
                    )
            );

            case "WF_LOGIN_PASSWORD_V2" -> new WorkflowDefinition(
                    "WF_LOGIN_PASSWORD_V2",
                    new WorkflowVersion(1, 0),
                    List.of(
                            new StepConfig("normalize", "NORMALIZE_IDENTIFIER", Map.of()),
                            new StepConfig("lookup",    "LOGIN_LOOKUP",         Map.of()),
                            new StepConfig("normalize", "NORMALIZE_IDENTIFIER", Map.of()),
                            new StepConfig("pwd",       "LOGIN_VERIFY_PASSWORD", Map.of()),
                            new StepConfig("token",     "ISSUE_ACCESS_TOKEN", Map.of(
                                    "accessTtlSeconds", 3600,
                                    "refreshTtlSeconds", 30 * 24 * 3600,
                                    "includeRefresh", true
                            )),
                            new StepConfig("device",    "DEVICE_UPSERT_SEEN", Map.of())
                    )
            );

            // 找回密码：start（按 identifier 发验证码 → HALT）
            case "WF_RESET_START_V1" -> new WorkflowDefinition(
                    "WF_RESET_START_V1",
                    new WorkflowVersion(1, 0),
                    List.of(
                            new StepConfig("normalize", "NORMALIZE_IDENTIFIER", Map.of()),
                            new StepConfig("resetSend", "OTP_SEND_RESET", Map.of("ttlSeconds", 300))
                    )
            );

            // 找回密码：verify（校验 OTP → 签发 actionToken → HALT/或 DONE）
            case "WF_RESET_VERIFY_V1" -> new WorkflowDefinition(
                    "WF_RESET_VERIFY_V1",
                    new WorkflowVersion(1, 0),
                    List.of(
                            new StepConfig("verify", "OTP_VERIFY_RESET", Map.of()),
                            new StepConfig("issue",  "ACTION_TOKEN_ISSUE_RESET", Map.of("ttlSeconds", 900))
                            // issue 返回 HALT 给前端，携带 resetTokenHash 或 token（看你实现）
                    )
            );

            // 找回密码：commit（消费 actionToken → 重置密码 → DONE）
            case "WF_RESET_COMMIT_V1" -> new WorkflowDefinition(
                    "WF_RESET_COMMIT_V1",
                    new WorkflowVersion(1, 0),
                    List.of(
                            new StepConfig("consume", "ACTION_TOKEN_CONSUME_RESET", Map.of()),
                            new StepConfig("setPwd",  "PASSWORD_SET_NEW", Map.of())
                    )
            );

            default -> throw new IllegalArgumentException("Unknown workflowId: " + workflowId);
        };
    }

    @Override
    public StepFactory stepFactory() {
        return factory;
    }

    /** 把 stepType 映射到具体 Step 类（节点库） */
    static class AuthStepFactory implements StepFactory {
        @Override
        public WorkflowStep create(StepConfig cfg) {
            return switch (cfg.stepType()) {
                case "NORMALIZE_IDENTIFIER" -> new NormalizeIdentifierStep(cfg);

                case "REGISTER_CREATE_USER" -> new RegisterCreateUserStep(cfg);
                case "OTP_SEND_REGISTER"    -> new OtpSendRegisterStep(cfg);
                case "OTP_VERIFY_REGISTER"  -> new OtpVerifyRegisterStep(cfg);
                case "IDENTIFIER_MARK_VERIFIED" -> new IdentifierMarkVerifiedStep(cfg);

                case "LOGIN_LOOKUP"         -> new LoginLookupStep(cfg);
                case "LOGIN_VERIFY_PASSWORD"-> new LoginVerifyPasswordStep(cfg);
                case "DEVICE_UPSERT_SEEN"   -> new DeviceUpsertSeenStep(cfg);

                case "OTP_SEND_RESET"       -> new OtpSendResetStep(cfg);
                case "OTP_VERIFY_RESET"     -> new OtpVerifyResetStep(cfg);
                case "ACTION_TOKEN_ISSUE_RESET"   -> new ActionTokenIssueResetStep(cfg);
                case "ACTION_TOKEN_CONSUME_RESET" -> new ActionTokenConsumeResetStep(cfg);
                case "PASSWORD_SET_NEW"           -> new PasswordSetNewStep(cfg);
                case "ISSUE_ACCESS_TOKEN" -> new IssueAccessTokenStep(cfg);

                default -> throw new IllegalArgumentException("Unknown stepType: " + cfg.stepType());
            };
        }
    }
}