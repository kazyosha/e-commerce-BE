package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.product.ProductCreateRequest;
import com.c05.kaz.ecommercebackend.dto.product.ProductResponse;
import com.c05.kaz.ecommercebackend.services.SupplierProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SupplierProductController {

    private final SupplierProductService supplierProductService;
    private final ObjectMapper objectMapper;

    /**
     * Chức năng 14: Nhà cung cấp thêm 1 sản phẩm để kinh doanh
     *
     * POST /api/suppliers/products
     * Content-Type: multipart/form-data
     *
     * parts:
     *  - data: JSON (ProductCreateRequest)
     *      {
     *          "categoryIds": [1, 3, 5],
     *          "name": "...",
     *          "description": "...",
     *          "price": 100000,
     *          "quantity": 10
     *      }
     *  - images: list file ảnh (MultipartFile[])
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("data") @Valid ProductCreateRequest request,
            @RequestPart("images") MultipartFile[] images
    ) {
        ProductResponse response = supplierProductService.createProduct(request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Nhà cung cấp xem danh sách sản phẩm của mình (phân trang + search + lọc danh mục + khoảng giá)
     *
     * GET /api/suppliers/products
     *
     * Query params:
     *  - page (default 0)
     *  - size (default 10)
     *  - search: tìm theo tên sản phẩm (optional)
     *  - categoryId: lọc theo 1 danh mục (optional)
     *      -> BE sẽ tìm các product có chứa category này trong danh sách categories
     *  - minPrice, maxPrice: lọc theo khoảng giá (optional)
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<Long> categoryIds,  // ⭐
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice
    ) {
        Page<ProductResponse> result = supplierProductService.getMyProducts(
                page, size, search, categoryIds, minPrice, maxPrice   // ⭐
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy chi tiết 1 sản phẩm của supplier (dùng cho trang edit)
     *
     * GET /api/suppliers/products/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ProductResponse> getMyProduct(@PathVariable Long id) {
        ProductResponse response = supplierProductService.getMyProductDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật sản phẩm (thông tin + ảnh)
     *
     * PUT /api/suppliers/products/{id}
     * Content-Type: multipart/form-data
     *
     * parts:
     *  - data: JSON (ProductCreateRequest)
     *      {
     *          "categoryIds": [1, 3],
     *          "name": "...",
     *          "description": "...",
     *          "price": 100000,
     *          "quantity": 10
     *      }
     *  - newImages: list ảnh mới (optional)
     *  - keepImages: JSON array các URL ảnh cũ muốn giữ lại (optional)
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart("data") @Valid ProductCreateRequest request,
            @RequestPart(value = "newImages", required = false) MultipartFile[] newImages,
            @RequestPart(value = "keepImages", required = false) String keepImagesJson
    ) {
        List<String> keepImages = Collections.emptyList();

        try {
            if (keepImagesJson != null && !keepImagesJson.isBlank()) {
                keepImages = objectMapper.readValue(
                        keepImagesJson,
                        new TypeReference<List<String>>() {}
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Dữ liệu keepImages không hợp lệ", e);
        }

        ProductResponse response =
                supplierProductService.updateProduct(id, request, newImages, keepImages);

        return ResponseEntity.ok(response);
    }

    /**
     * Toggle trạng thái sản phẩm (đang bán <-> ngừng bán)
     *
     * PATCH /api/suppliers/products/{id}/toggle-active
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ProductResponse> toggleActive(@PathVariable Long id) {
        ProductResponse response = supplierProductService.toggleActive(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Chức năng 17: Nhà cung cấp chỉnh sửa riêng hình ảnh sản phẩm
     *
     * PUT /api/suppliers/products/{id}/images
     * Content-Type: multipart/form-data
     *
     * parts:
     *  - imageIdsToDelete: JSON array các ID ảnh muốn xoá (optional)
     *      ví dụ: "[1, 2, 5]"
     *  - newImages: list file ảnh mới (optional)
     */
    @PutMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<ProductResponse> updateProductImages(
            @PathVariable Long id,
            @RequestPart(value = "imageIdsToDelete", required = false) String imageIdsToDeleteJson,
            @RequestPart(value = "newImages", required = false) MultipartFile[] newImages
    ) {
        List<Long> imageIdsToDelete = List.of();

        try {
            if (imageIdsToDeleteJson != null && !imageIdsToDeleteJson.isBlank()) {
                imageIdsToDelete = objectMapper.readValue(
                        imageIdsToDeleteJson,
                        new TypeReference<List<Long>>() {}
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Dữ liệu imageIdsToDelete không hợp lệ", e);
        }

        ProductResponse response =
                supplierProductService.updateProductImages(id, imageIdsToDelete, newImages);

        return ResponseEntity.ok(response);
    }

}
