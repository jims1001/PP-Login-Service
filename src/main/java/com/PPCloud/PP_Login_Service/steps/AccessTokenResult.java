package com.PPCloud.PP_Login_Service.steps;

public record AccessTokenResult(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken
) {}