package com.c05.kaz.ecommercebackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employee_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeProfile {

    @Id
    @Column(name = "user_id")
    private Long id; // Trùng với users.id

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
    private String email;

    @Column(length = 255)
    private String address;

    @Column(length = 500)
    private String avatarUrl;

    private Long salary;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<ViolationNote> createdViolationNotes = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void beforeSave() {
        // Gán createdAt / updatedAt
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();

        // ✅ Đồng bộ email từ UserAccount
        if (user != null && user.getEmail() != null) {
            this.email = user.getEmail();
        }
    }
}
