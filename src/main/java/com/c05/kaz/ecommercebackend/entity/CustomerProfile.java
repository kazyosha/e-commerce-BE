package com.c05.kaz.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile {

    @Id
    private Long id; // shared PK với user nếu dùng @MapsId

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(length = 150)
    private String fullName;

    private java.time.LocalDate birthDate;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    private Integer provinceId;
    private Integer districtId;

    @Column(length = 20)
    private String wardCode;

    private String avatarUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
