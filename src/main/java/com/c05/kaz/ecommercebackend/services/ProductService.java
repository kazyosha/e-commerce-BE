package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.entity.Category;
import com.c05.kaz.ecommercebackend.entity.Product;
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

    public Optional<Product> getById(Long id) {
        return productRepository.findById(id);
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
}
