package com.c05.kaz.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerProfile {

    @Id
    private Long id; // trùng với user_id

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(length = 150)
    private String fullName;

    private LocalDate birthDate;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    // mối quan tâm: many-to-many với Category
    @ManyToMany
    @JoinTable(name = "customer_interests",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> interests = new HashSet<>();

    private String avatarUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
