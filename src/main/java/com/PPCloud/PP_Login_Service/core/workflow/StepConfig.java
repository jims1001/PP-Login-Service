package com.PPCloud.PP_Login_Service.core.workflow;

import java.util.Map;
/**
 * StepConfig：一个节点的配置（未来可从 DB 读）
 *
 * - stepId：节点逻辑名（用于可观测/审计）
 * - stepType：节点类型（用于 StepFactory 实例化）
 * - params：节点参数（例如 OTP ttlSeconds、策略开关等）
 */
public record StepConfig(
        String stepId,
        String stepType,
        Map<String, Object> params
) {}