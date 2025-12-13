package com.PPCloud.PP_Login_Service.security;

public record TokenPair(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken
) {}