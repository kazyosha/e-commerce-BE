package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // ⭐ dùng cho update ảnh
    List<ProductImage> findByProduct_Id(Long productId);
}
