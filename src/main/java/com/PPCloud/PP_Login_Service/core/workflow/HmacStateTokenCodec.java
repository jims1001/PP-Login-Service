package com.PPCloud.PP_Login_Service.core.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@Component("hmacStateTokenCodec")
public class HmacStateTokenCodec implements StateCodec {

    private final ObjectMapper om;
    private final byte[] secret; // 来自配置
    private final Base64.Encoder b64UrlEnc = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder b64UrlDec = Base64.getUrlDecoder();

    public HmacStateTokenCodec(ObjectMapper om, @Value("${pp.workflow.state-token.secret}") String secret) {
        this.om = om;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 16) {
            throw new IllegalArgumentException("stateToken secret too short (>=16 bytes recommended)");
        }
    }

    @Override
    public String encode(WorkflowState state) {
        try {
            byte[] json = om.writeValueAsBytes(state);
            String payload = b64UrlEnc.encodeToString(json);
            String sig = b64UrlEnc.encodeToString(hmacSha256(payload.getBytes(StandardCharsets.UTF_8)));
            // token 格式：payload.sig
            return payload + "." + sig;
        } catch (Exception e) {
            throw new RuntimeException("STATE_TOKEN_ENCODE_FAILED", e);
        }
    }

    @Override
    public WorkflowState decode(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                throw new IllegalArgumentException("STATE_TOKEN_BAD_FORMAT");
            }
            String payload = parts[0];
            String sig = parts[1];

            byte[] expected = hmacSha256(payload.getBytes(StandardCharsets.UTF_8));
            byte[] actual = b64UrlDec.decode(sig);

            if (!MessageDigest.isEqual(expected, actual)) {
                throw new IllegalArgumentException("STATE_TOKEN_BAD_SIGNATURE");
            }

            byte[] json = b64UrlDec.decode(payload);

            WorkflowState state = om.readValue(json, WorkflowState.class);
            log.info("STATE_TOKEN_DECODED: {}", state);
            return state;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("STATE_TOKEN_DECODE_FAILED", e);
        }
    }

    private byte[] hmacSha256(byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(data);
    }
}