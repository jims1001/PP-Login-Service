package com.PPCloud.PP_Login_Service.port;

/** PasswordHasher：hash 与 verify（argon2/bcrypt 等） */
public interface PasswordHasher {
    String hash(String passwordRaw);
    boolean verify(String passwordRaw, String passwordHash);
}