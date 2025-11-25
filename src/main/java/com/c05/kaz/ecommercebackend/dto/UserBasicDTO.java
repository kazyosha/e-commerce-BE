package com.c05.kaz.ecommercebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserBasicDTO {

    private Long id;
    private String fullName;
    private String username;
    private String shopName;
    private String avatarUrl;
}
