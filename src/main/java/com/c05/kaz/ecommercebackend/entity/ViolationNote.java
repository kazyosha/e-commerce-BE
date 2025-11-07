package com.c05.kaz.ecommercebackend.entity;

import com.c05.kaz.ecommercebackend.enums.ViolationLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "violation_notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViolationNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // người tạo: nhân sự
    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by_employee_id")
    private EmployeeProfile createdBy;

    // đối tượng vi phạm: có thể là khách hoặc shop (có thể null một trong hai)
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private SupplierShop supplier;

    @Enumerated(EnumType.STRING)
    private ViolationLevel level;

    @Column(nullable = false, length = 2000)
    private String content;

    private LocalDateTime createdAt;
}

