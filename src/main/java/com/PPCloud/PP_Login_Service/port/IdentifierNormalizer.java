package com.PPCloud.PP_Login_Service.port;

/** IdentifierNormalizer：username/email/phone 规范化 */
public interface IdentifierNormalizer {
    String normalize(String type, String raw);
}