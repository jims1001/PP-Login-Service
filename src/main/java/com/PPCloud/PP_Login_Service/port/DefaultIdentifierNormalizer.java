package com.PPCloud.PP_Login_Service.port;

import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * DefaultIdentifierNormalizer
 *
 * 当前支持：
 * - EMAIL
 * - USERNAME
 * - PHONE（简化版，生产建议用 libphonenumber）
 * - EXTERNAL（第三方 subject）
 *
 * 所有 normalize 的输出都应该：
 * - 非 null
 * - 无首尾空格
 * - 稳定、可重复
 */
@Service
public class DefaultIdentifierNormalizer implements IdentifierNormalizer {

    @Override
    public String normalize(String type, String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("identifier raw value is null");
        }

        String v = raw.trim();

        if (v.isEmpty()) {
            throw new IllegalArgumentException("identifier raw value is empty");
        }

        return switch (type) {
            case "EMAIL" -> normalizeEmail(v);
            case "USERNAME" -> normalizeUsername(v);
            case "PHONE" -> normalizePhone(v);
            case "EXTERNAL" -> normalizeExternal(v);
            default -> throw new IllegalArgumentException("unsupported identifier type: " + type);
        };
    }

    // ---------------- email ----------------

    /**
     * EMAIL 规范化规则：
     * - trim
     * - 全部转小写
     * - 不做 gmail 点号/加号裁剪（⚠️ 不同邮箱语义不同）
     */
    private String normalizeEmail(String v) {
        return v.toLowerCase(Locale.ROOT);
    }

    // ---------------- username ----------------

    /**
     * USERNAME 规范化规则：
     * - trim
     * - 小写
     *
     * ⚠️ 注意：
     * - 如果你希望 username 区分大小写，这里就不要 toLowerCase
     * - 但大多数系统不区分
     */
    private String normalizeUsername(String v) {
        return v.toLowerCase(Locale.ROOT);
    }

    // ---------------- phone ----------------

    /**
     * PHONE（简化版）：
     * - 去空格、-、()
     * - 确保 + 开头
     *
     * ⚠️ 强烈建议生产环境用：
     *   Google libphonenumber
     */
    private String normalizePhone(String v) {
        String digits = v.replaceAll("[\\s\\-()]", "");

        if (!digits.startsWith("+")) {
            // 默认国家码策略（示例：+86）
            digits = "+86" + digits;
        }

        return digits;
    }

    // ---------------- external ----------------

    /**
     * EXTERNAL（OAuth / OIDC subject）：
     * - 保持原样
     * - trim
     *
     * ⚠️ subject 本身已是稳定标识
     */
    private String normalizeExternal(String v) {
        return v;
    }
}