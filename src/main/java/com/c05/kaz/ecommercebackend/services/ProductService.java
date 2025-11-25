package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.product.ProductDetailResponse;
import com.c05.kaz.ecommercebackend.dto.product.ProductResponse;
import com.c05.kaz.ecommercebackend.dto.product.ProductSimpleResponse;
import com.c05.kaz.ecommercebackend.entity.Category;
import com.c05.kaz.ecommercebackend.entity.Product;
import com.c05.kaz.ecommercebackend.entity.ProductImage;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.repository.CategoryRepository;
import com.c05.kaz.ecommercebackend.repository.ProductRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public Page<Product> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findAll(pageable);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .filter(Product::isActive)  // nếu chỉ lấy sản phẩm active
                .map(this::toResponse)
                .toList();
    }

    private ProductResponse toResponse(Product p) {

        List<Long> categoryIds = p.getCategories().stream()
                .map(Category::getId)
                .toList();

        List<String> categoryNames = p.getCategories().stream()
                .map(Category::getName)
                .toList();

        return ProductResponse.builder()
                .id(p.getId())
                .supplierId(p.getSupplier().getId())
                .supplierName(p.getSupplier().getShopName())

                .categoryIds(categoryIds)
                .categoryNames(categoryNames)

                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .importPrice(p.getImportPrice())
                .quantity(p.getQuantity())
                .active(p.isActive())

                .thumbnailUrl(p.getEffectiveThumbnail())
                .images(
                        p.getImages() == null ? List.of()
                                : p.getImages().stream()
                                .map(ProductImage::getImageUrl)
                                .toList()
                )

                .soldQuantity(p.getSoldQuantity())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())

                .build();
    }


    public Page<ProductResponse> getHomeProducts(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Product> products = productRepository.findByActiveTrue(pageable);

        return products.map(p -> {

            // lấy ảnh đầu tiên làm thumbnail
            String thumbnail = null;
            if (p.getImages() != null && !p.getImages().isEmpty()) {
                thumbnail = p.getImages().get(0).getImageUrl();
            }

            return ProductResponse.builder()
                    .id(p.getId())
                    .supplierId(p.getSupplier().getId())
                    .supplierName(p.getSupplier().getShopName())

                    .categoryIds(
                            p.getCategories().stream()
                                    .map(Category::getId)
                                    .toList()
                    )
                    .categoryNames(
                            p.getCategories().stream()
                                    .map(Category::getName)
                                    .toList()
                    )

                    .name(p.getName())
                    .description(p.getDescription())
                    .price(p.getPrice())
                    .importPrice(p.getImportPrice())
                    .quantity(p.getQuantity())
                    .active(p.isActive())

                    .thumbnailUrl(p.getEffectiveThumbnail())
                    .images(
                            p.getImages().stream()
                                    .map(ProductImage::getImageUrl)
                                    .toList()
                    )
                    .soldQuantity(p.getSoldQuantity())
                    .createdAt(p.getCreatedAt())
                    .updatedAt(p.getUpdatedAt())

                    .build();
        });
    }

    /**
     * Search sản phẩm cho phía customer:
     *  - keyword: tìm theo tên
     *  - categoryId: lọc theo 1 danh mục (product có chứa category đó trong danh sách categories)
     */
    public Page<Product> search(String keyword, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String kw = (keyword == null) ? "" : keyword.trim();

        // Không keyword, không category -> trả về all
        if (kw.isEmpty() && categoryId == null) {
            return getAllProducts(page, size);
        }

        // Có keyword, không category -> search theo tên
        if (!kw.isEmpty() && categoryId == null) {
            return productRepository.findByNameContainingIgnoreCase(kw, pageable);
        }

        // Có category, không keyword -> lọc theo category
        if (kw.isEmpty()) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category == null) {
                return Page.empty(pageable);
            }
            // many-to-many
            return productRepository.findDistinctByCategories(category, pageable);
        }

        // Có cả keyword & category
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return Page.empty(pageable);
        }
        // many-to-many
        return productRepository.findDistinctByNameContainingIgnoreCaseAndCategories(
                kw,
                category,
                pageable
        );
    }

    /**
     * Lấy top sản phẩm bán chạy theo shop
     */
    public List<Product> getTopSoldByShop(Long shopId, int limit) {
        SupplierShop shop = supplierRepository.findById(shopId).orElse(null);
        if (shop == null) return List.of();

        Pageable pageable = PageRequest.of(0, limit, Sort.by("soldQuantity").descending());
        return productRepository.findBySupplier(shop, pageable).getContent();
    }

    public ProductDetailResponse getProductDetail(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Lấy category đầu tiên
        Long categoryId = null;
        String categoryName = null;

        if (p.getCategories() != null && !p.getCategories().isEmpty()) {
            Category cat = p.getCategories().get(0);
            categoryId = cat.getId();
            categoryName = cat.getName();
        }

        return ProductDetailResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .quantity(p.getQuantity())
                .active(p.isActive())
                .soldQuantity(p.getSoldQuantity())

                // *** SỬA ĐÚNG Ở ĐÂY ***
                .categoryId(categoryId)
                .categoryName(categoryName)

                .supplierId(p.getSupplier().getId())
                .supplierName(p.getSupplier().getShopName())

                .images(
                        p.getImages() == null
                                ? java.util.List.of()
                                : p.getImages().stream()
                                .map(img -> img.getImageUrl())
                                .toList()
                )
                .build();
    }



    public List<ProductSimpleResponse> getRelatedByCategory(Long categoryId, Long excludeId) {
        return productRepository.findRelatedByCategory(categoryId, excludeId)
                .stream()
                .map(this::toSimple)
                .toList();
    }

    public List<ProductSimpleResponse> getBySupplier(Long supplierId, Long excludeId) {
        return productRepository.findBySupplierExcept(supplierId, excludeId)
                .stream()
                .map(this::toSimple)
                .toList();
    }

    private ProductSimpleResponse toSimple(Product p) {
        String thumbnail = null;

        if (p.getImages() != null && !p.getImages().isEmpty()) {
            thumbnail = p.getImages().get(0).getImageUrl();
        }

        return ProductSimpleResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .thumbnailUrl(thumbnail)
                .soldQuantity(p.getSoldQuantity())
                .build();
    }

}
