package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.product.*;
import com.c05.kaz.ecommercebackend.dto.supplier.ProductTopResponse;
import com.c05.kaz.ecommercebackend.entity.Category;
import com.c05.kaz.ecommercebackend.entity.Product;
import com.c05.kaz.ecommercebackend.entity.ProductImage;
import com.c05.kaz.ecommercebackend.entity.SupplierShop;
import com.c05.kaz.ecommercebackend.repository.CategoryRepository;
import com.c05.kaz.ecommercebackend.repository.ProductRepository;
import com.c05.kaz.ecommercebackend.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

        return products.map(p -> ProductResponse.builder()
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
                .build());
    }

    // ========================
    // CUSTOMER SEARCH (old API)
    // ========================
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
            if (category == null) return Page.empty(pageable);

            return productRepository.findDistinctByCategories(category, pageable);
        }

        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) return Page.empty(pageable);

        return productRepository
                .findDistinctByNameContainingIgnoreCaseAndCategories(kw, category, pageable);
    }

    // ========================
    // TOP SOLD PRODUCTS
    // ========================
    public List<Product> getTopSoldByShop(Long shopId, int limit) {
        SupplierShop shop = supplierRepository.findById(shopId).orElse(null);
        if (shop == null) return List.of();

        Pageable pageable = PageRequest.of(0, limit, Sort.by("soldQuantity").descending());
        return productRepository.findBySupplier(shop, pageable).getContent();
    }

    // ========================
    // PRODUCT DETAIL
    // ========================
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

                // FIX BUG: lấy đúng category
                .categoryId(
                        p.getCategories().isEmpty()
                                ? null
                                : p.getCategories().get(0).getId()
                )
                .categoryName(
                        p.getCategories().isEmpty()
                                ? null
                                : p.getCategories().get(0).getName()
                )

                // *** SỬA ĐÚNG Ở ĐÂY ***
                .categoryId(categoryId)
                .categoryName(categoryName)

                .supplierId(p.getSupplier().getId())
                .supplierName(p.getSupplier().getShopName())
                .supplierAvatar(p.getSupplier().getAvatarUrl())
                .supplierDescription(p.getSupplier().getDescription())
                .supplierAddress(p.getSupplier().getAddress())

                .images(
                        p.getImages().stream()
                                .map(ProductImage::getImageUrl)
                                .toList()
                )
                .build();
    }

    // ========================
    // PUBLIC SEARCH (NEW)
    // ========================
    public Page<ProductResponse> searchPublic(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Product> result =
                productRepository.searchPublicProducts(keyword, pageable);

        return result.map(p -> ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .thumbnailUrl(
                        p.getEffectiveThumbnail()
                )
                .build());
    }
    public List<String> suggest(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();
        return productRepository
                .findTop10ByNameContainingIgnoreCase(keyword.trim())
                .stream()
                .map(Product::getName)
                .toList();
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
    public Page<ProductListItemDTO> advancedSearch(AdvancedProductFilterDTO f) {

        Pageable pageable = PageRequest.of(
                f.getPage(),
                f.getSize(),
                buildSort(f.getSort())
        );

        Page<Product> pageData = productRepository.advancedSearch(
                normalize(f.getSearch()),
                normalize(f.getCategory()),
                normalize(f.getLocation()),
                f.getMinPrice(),
                f.getMaxPrice(),
                f.getRating(),
                pageable
        );

        return pageData.map(p -> ProductListItemDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .thumbnailUrl(p.getEffectiveThumbnail())
                .soldQuantity(p.getSoldQuantity())
                .avgRating(p.getAvgRating())
                .shopName(p.getSupplier().getShopName())
                .shopAddress(p.getSupplier().getAddress())
                .build()
        );
    }
    private Sort buildSort(String sort) {
        if (sort == null || sort.isBlank() || sort.equals("popular")) {
            return Sort.by(Sort.Direction.DESC, "soldQuantity");
        }

        return switch (sort) {
            case "lowToHigh" -> Sort.by(Sort.Direction.ASC, "price");
            case "highToLow" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "soldQuantity");
        };
    }
    private String normalize(String s) {
        if (s == null) return null;
        s = s.trim();
        return (s.isEmpty()) ? null : s;
    }

    public List<ProductResponse> getBySupplierAll(Long supplierId) {
        return productRepository.findBySupplierId(supplierId)
                .stream()
                .filter(Product::isActive)
                .map(this::toResponse)
                .toList();
    }

    public List<ProductTopResponse> getTop5ByShop(Long shopId) {
        return productRepository.findTop5BySupplierIdOrderBySoldQuantityDesc(shopId)
                .stream()
                .map(p -> new ProductTopResponse(
                        p.getId(),
                        p.getName(),
                        p.getPrice(),
                        p.getEffectiveThumbnail(),
                        Math.toIntExact(p.getSoldQuantity())
                ))
                .toList();
    }

}
