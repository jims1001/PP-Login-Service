package com.PPCloud.PP_Login_Service.security;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * 先用进程内随机 key（开发环境）。
 * 生产：你可以改成从配置/DB/KMS 加载，并做 key rotation。
 */
@Component
public class TokenKeyProvider {

    private final RSAKey rsa;

    public TokenKeyProvider() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            KeyPair kp = g.generateKeyPair();
            this.rsa = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                    .privateKey((RSAPrivateKey) kp.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("INIT_RSA_KEY_FAILED", e);
        }
    }

    public RSAKey signingKey() {
        return rsa;
    }
}