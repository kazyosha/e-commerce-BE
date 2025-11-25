package com.c05.kaz.ecommercebackend.entity;

import com.c05.kaz.ecommercebackend.enums.SupplierStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "suppliers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierShop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // trùng với user_id

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false, length = 150)
    private String shopName;

    @Column(length = 500)
    private String description;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String avatarUrl;

    @Column(length = 255)
    private String mapLocation;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierDocument> documents;

    @OneToMany(mappedBy = "supplier")
    @JsonIgnore
    private List<Product> products;

    @OneToMany(mappedBy = "supplier")
    @JsonIgnore
    private List<Promotion> promotions;

    @OneToMany(mappedBy = "supplier")
    @JsonIgnore
    private List<Order> orders;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private SupplierStatus status;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = SupplierStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
