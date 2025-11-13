package com.c05.kaz.ecommercebackend.entity;

import com.c05.kaz.ecommercebackend.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mỗi document thuộc 1 supplier
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    @JsonIgnore
    private SupplierShop supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentType type;

    private String publicId;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    private LocalDateTime uploadedAt;

    // Cho phép admin duyệt từng chứng từ nếu muốn
    private Boolean approved;
    private String rejectReason;
}

