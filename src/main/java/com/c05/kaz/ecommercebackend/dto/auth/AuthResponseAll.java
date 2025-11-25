package com.c05.kaz.ecommercebackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponseAll {
    private String token;
    private UserDTO user;
}
