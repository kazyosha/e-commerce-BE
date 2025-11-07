package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
}
