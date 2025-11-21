package com.c05.kaz.ecommercebackend.dto.user;

import lombok.Data;

@Data
public class SocialLoginRequest {
    private String provider;    // "GOOGLE" hoặc "FACEBOOK"
    private String token;
    private String credential;// Google ID token, hoặc Facebook access token
}
