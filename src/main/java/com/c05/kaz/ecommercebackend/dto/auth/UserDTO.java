package com.c05.kaz.ecommercebackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String userType;
    private Object roles;
}
