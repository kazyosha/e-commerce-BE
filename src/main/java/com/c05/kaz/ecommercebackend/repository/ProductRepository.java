package com.c05.kaz.ecommercebackend.repository;

import com.c05.kaz.ecommercebackend.entity.Category;
import com.c05.kaz.ecommercebackend.entity.Product;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByActiveTrue(Pageable pageable);

    // ==========================
    // EXISTING QUERIES (ĐÃ ADAPT MANY-TO-MANY)
    // ==========================

    // Lấy tất cả sản phẩm (phân trang) – dùng cho Admin / Customer
    Page<Product> findAll(Pageable pageable);

    // Tìm sản phẩm theo tên (không phân biệt hoa thường) – toàn hệ thống
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Lọc theo danh mục – toàn hệ thống (many-to-many)
    Page<Product> findDistinctByCategories(Category category, Pageable pageable);

    // Lọc theo categoryId – toàn hệ thống (many-to-many)
    Page<Product> findDistinctByCategories_Id(Long categoryId, Pageable pageable);

    // Tìm theo tên + danh mục – toàn hệ thống (many-to-many)
    Page<Product> findDistinctByNameContainingIgnoreCaseAndCategories(
            String name,
            Category category,
            Pageable pageable
    );

    // Tìm theo tên + categoryId – toàn hệ thống (many-to-many)
    Page<Product> findDistinctByNameContainingIgnoreCaseAndCategories_Id(
            String name,
            Long categoryId,
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
     * Lọc sản phẩm theo DANH MỤC trong shop của 1 supplier cụ thể (many-to-many)
     */
    Page<Product> findDistinctBySupplier_IdAndCategories_Id(
            Long supplierId,
            Long categoryId,
            Pageable pageable
    );

    /**
     * Kết hợp: TÊN + DANH MỤC trong shop của 1 supplier cụ thể (many-to-many)
     */
    Page<Product> findDistinctBySupplier_IdAndNameContainingIgnoreCaseAndCategories_Id(
            Long supplierId,
            String name,
            Long categoryId,
            Pageable pageable
    );

    // ==========================
    // MỚI: TÊN + DANH MỤC + KHOẢNG GIÁ TRONG SHOP SUPPLIER (many-to-many)
    // ==========================

    @Query("""
       SELECT DISTINCT p FROM Product p
       LEFT JOIN p.categories c
       WHERE p.supplier.id = :supplierId
         AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
         AND (:minPrice IS NULL OR p.price >= :minPrice)
         AND (:maxPrice IS NULL OR p.price <= :maxPrice)
         AND (:categoryIds IS NULL OR c.id IN :categoryIds)
       """)
    Page<Product> searchMyProducts(
            @Param("supplierId") Long supplierId,
            @Param("name") String name,
            @Param("categoryIds") List<Long> categoryIds,   // ⭐
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable
    );

    // PUBLIC SEARCH (Customer search)
// ==========================
    @Query("""
                SELECT p FROM Product p
                WHERE 
                    LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Product> searchPublicProducts(
            @Param("keyword") String keyword,
            Pageable pageable
    );
    List<Product> findTop10ByNameContainingIgnoreCase(String keyword);


    @Query("""
    SELECT DISTINCT p
    FROM Product p
    JOIN p.categories c
    WHERE c.id = :categoryId
      AND p.id <> :excludeId
      AND p.active = true
""")
    List<Product> findRelatedByCategory(Long categoryId, Long excludeId);


    @Query("SELECT p FROM Product p WHERE p.supplier.id = :supplierId AND p.id <> :excludeId AND p.active = true")
    List<Product> findBySupplierExcept(Long supplierId, Long excludeId);

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.categories c
    WHERE (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
      AND (:category IS NULL OR LOWER(c.name) = LOWER(:category))
      AND (:location IS NULL OR LOWER(p.supplier.address) LIKE LOWER(CONCAT('%', :location, '%')))
      AND (:minPrice IS NULL OR p.price >= :minPrice)
      AND (:maxPrice IS NULL OR p.price <= :maxPrice)
      AND (:rating IS NULL OR p.avgRating >= :rating)
""")
    Page<Product> advancedSearch(
            @Param("search") String search,
            @Param("category") String category,
            @Param("location") String location,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("rating") Integer rating,
            Pageable pageable
    );


    @Query("SELECT MAX(p.supplierProductIndex) FROM Product p WHERE p.supplier.id = :supplierId")
    Integer findMaxIndexBySupplier(Long supplierId);

    List<Product> findBySupplierId(Long supplierId);
    List<Product> findTop5BySupplierIdOrderBySoldQuantityDesc(Long supplierId);

}
