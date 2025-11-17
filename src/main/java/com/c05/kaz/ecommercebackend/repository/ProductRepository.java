package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.Category;
import com.c05.kaz.ecommercebackend.entity.Product;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByActiveTrue(Pageable pageable);

    // ==========================
    // EXISTING QUERIES (GIỮ NGUYÊN)
    // ==========================

    // Lấy tất cả sản phẩm (phân trang) – dùng cho Admin / Customer
    Page<Product> findAll(Pageable pageable);

    // Tìm sản phẩm theo tên (không phân biệt hoa thường) – toàn hệ thống
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Lọc theo danh mục – toàn hệ thống
    Page<Product> findByCategory(Category category, Pageable pageable);

    // Tìm theo tên + danh mục – toàn hệ thống
    Page<Product> findByNameContainingIgnoreCaseAndCategory(
            String name,
            Category category,
            Pageable pageable
    );

    // Lấy sản phẩm theo shop (supplier)
    Page<Product> findBySupplier(SupplierShop supplier, Pageable pageable);

    // Lấy sản phẩm theo supplierId (dùng cho supplier đang đăng nhập)
    Page<Product> findBySupplier_Id(Long supplierId, Pageable pageable);

    // ⭐ Dùng cho xem chi tiết & update sản phẩm của chính supplier
    Optional<Product> findByIdAndSupplier_Id(Long id, Long supplierId);

    // ==========================
    // TÌM KIẾM TRONG SHOP CỦA NHÀ CUNG CẤP (TÊN + DANH MỤC)
    // ==========================

    /**
     * Tìm sản phẩm theo TÊN trong shop của 1 supplier cụ thể
     */
    Page<Product> findBySupplier_IdAndNameContainingIgnoreCase(
            Long supplierId,
            String name,
            Pageable pageable
    );

    /**
     * Lọc sản phẩm theo DANH MỤC trong shop của 1 supplier cụ thể
     */
    Page<Product> findBySupplier_IdAndCategory_Id(
            Long supplierId,
            Long categoryId,
            Pageable pageable
    );

    /**
     * Kết hợp: TÊN + DANH MỤC trong shop của 1 supplier cụ thể
     */
    Page<Product> findBySupplier_IdAndNameContainingIgnoreCaseAndCategory_Id(
            Long supplierId,
            String name,
            Long categoryId,
            Pageable pageable
    );

    // ==========================
    // MỚI: TÊN + DANH MỤC + KHOẢNG GIÁ TRONG SHOP SUPPLIER
    // ==========================

    @Query("""
           SELECT p FROM Product p
           WHERE p.supplier.id = :supplierId
             AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
             AND (:categoryId IS NULL OR p.category.id = :categoryId)
             AND (:minPrice IS NULL OR p.price >= :minPrice)
             AND (:maxPrice IS NULL OR p.price <= :maxPrice)
           """)
    Page<Product> searchMyProducts(
            @Param("supplierId") Long supplierId,
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable
    );
}
