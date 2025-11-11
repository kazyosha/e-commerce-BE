package com.c05.kaz.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employee_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfile {

    @Id
    private Long id; // trùng với user_id

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false, length = 100)
    private String fullName;

    private Integer age;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    private Long salary;

    @OneToMany(mappedBy = "createdBy")
    @Builder.Default
    private List<ViolationNote> createdViolationNotes = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

