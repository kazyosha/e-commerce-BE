package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.product.ProductDetailResponse;
import com.c05.kaz.ecommercebackend.dto.product.ProductResponse;
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

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    // ========================
    // GET ALL PRODUCTS
    // ========================
    public Page<Product> getAllProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findAll(pageable);
    }

    // ========================
    // HOME PRODUCTS
    // ========================
    public Page<ProductResponse> getHomeProducts(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Product> products = productRepository.findByActiveTrue(pageable);

        return products.map(p -> ProductResponse.builder()
                .id(p.getId())
                .supplierId(p.getSupplier().getId())
                .supplierName(p.getSupplier().getShopName())

                .categoryIds(
                        p.getCategories().stream().map(Category::getId).toList()
                )
                .categoryName(
                        p.getCategories().stream().map(Category::getName).toList()
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

        if (kw.isEmpty() && categoryId == null) {
            return getAllProducts(page, size);
        }

        if (!kw.isEmpty() && categoryId == null) {
            return productRepository.findByNameContainingIgnoreCase(kw, pageable);
        }

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

                .supplierId(p.getSupplier().getId())
                .supplierName(p.getSupplier().getShopName())

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

}
