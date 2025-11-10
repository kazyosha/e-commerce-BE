package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.SupplierDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierDocumentRepository extends JpaRepository<SupplierDocument, Long> {
    List<SupplierDocument> findBySupplier_Id(Long supplierId);
}
