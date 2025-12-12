package com.PPCloud.PP_Login_Service.port;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * DelegatingPasswordHasher
 *
 * 存库格式示例：
 * - "{bcrypt}$2a$10$...."
 * - "{argon2}$argon2id$v=19$m=65536,t=3,p=1$...."
 *
 * verify 时根据前缀 "{...}" 选择对应 encoder。
 *
 * ✅ 好处：
 * - 你以后想把默认从 bcrypt 换成 argon2：只要改 defaultId
 * - 老用户仍可登录，因为 verify 会按 hash 前缀识别
 * - 登录成功后你可做“透明升级”（可选）
 */
@Service
public class DelegatingPasswordHasher implements PasswordHasher {
    private static final String defaultId = "bcrypt";
    private final Map<String, PasswordEncoder> encoders;

    public DelegatingPasswordHasher(  @Qualifier("passwordEncoders")  Map<String, PasswordEncoder> encoders) {
        if (!encoders.containsKey(defaultId)) {
            throw new IllegalArgumentException("defaultId not found in encoders: " + defaultId);
        }
        this.encoders = encoders;
    }

    @Override
    public String hash(String passwordRaw) {
        if (passwordRaw == null || passwordRaw.isBlank()) {
            throw new IllegalArgumentException("password is empty");
        }
        PasswordEncoder pe = encoders.get(defaultId);
        // 存储时写入算法 id 前缀，便于未来升级
        return "{" + defaultId + "}" + pe.encode(passwordRaw);
    }

    @Override
    public boolean verify(String passwordRaw, String passwordHash) {
        if (passwordRaw == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }

        // 解析 "{id}....."
        String id = parseId(passwordHash);
        String realHash = stripPrefix(passwordHash);

        PasswordEncoder pe = encoders.get(id);
        if (pe == null) {
            // 不认识的算法：直接失败（同时记审计）
            return false;
        }
        return pe.matches(passwordRaw, realHash);
    }

    /** 是否需要升级 hash（可选：登录成功后透明升级） */
    public boolean needsRehash(String passwordHash) {
        String id = parseId(passwordHash);
        return !defaultId.equals(id);
    }

    private String parseId(String passwordHash) {
        if (passwordHash.startsWith("{")) {
            int idx = passwordHash.indexOf('}');
            if (idx > 1) return passwordHash.substring(1, idx);
        }
        // 兼容：老数据没前缀时，默认当 bcrypt（你也可以直接当作 fail）
        return "bcrypt";
    }

    private String stripPrefix(String passwordHash) {
        if (passwordHash.startsWith("{")) {
            int idx = passwordHash.indexOf('}');
            if (idx > 0 && idx + 1 < passwordHash.length()) {
                return passwordHash.substring(idx + 1);
            }
        }
        return passwordHash;
    }

    // -------- 工厂方法：给你一套默认参数 --------

    public static DelegatingPasswordHasher defaultBcrypt() {
        // BCrypt strength：10~12 常用，越大越慢
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(12);

        // Argon2 参数（如果你未来要用 argon2，直接切 defaultId 即可）
        // Spring 的 Argon2PasswordEncoder 参数含义：
        // saltLength, hashLength, parallelism, memory（KB）, iterations
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(
                16, 32, 1, 65536, 3
        );

        return new DelegatingPasswordHasher(
                Map.of(
                        "bcrypt", bcrypt,
                        "argon2", argon2
                )
        );
    }
}