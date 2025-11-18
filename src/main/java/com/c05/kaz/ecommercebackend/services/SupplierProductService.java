package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.product.ProductCreateRequest;
import com.c05.kaz.ecommercebackend.dto.product.ProductResponse;
import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.*;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SupplierProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final UserAccountRepository userAccountRepository;
    private final Cloudinary cloudinary;

    // =====================================
    //           HELPER METHODS
    // =====================================

    /**
     * Lấy UserAccount hiện tại từ SecurityContext
     */
    private UserAccount getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
    }

    /**
     * Lấy shop của supplier hiện tại, đồng thời kiểm tra userType
     */
    private SupplierShop getCurrentSupplierShopOrThrow() {
        UserAccount current = getCurrentUser();

        if (current.getUserType() != UserType.SUPPLIER) {
            throw new AccessDeniedException("Chỉ nhà cung cấp mới được phép thực hiện thao tác này");
        }

        return supplierRepository.findByUser_Id(current.getId())
                .orElseThrow(() -> new RuntimeException("Supplier chưa có shop"));
    }

    // =====================================
    //           CREATE PRODUCT
    // =====================================

    /**
     * Nhà cung cấp thêm 1 sản phẩm để kinh doanh
     */
    public ProductResponse createProduct(ProductCreateRequest request, MultipartFile[] images) {

        if (images == null || images.length == 0) {
            throw new RuntimeException("Phải upload ít nhất 1 ảnh sản phẩm");
        }

        SupplierShop supplierShop = getCurrentSupplierShopOrThrow();

        // 1. Lấy danh sách category từ request
        if (request.getCategoryIds() == null || request.getCategoryIds().isEmpty()) {
            throw new RuntimeException("Phải chọn ít nhất 1 danh mục");
        }

        List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
        if (categories.isEmpty()) {
            throw new RuntimeException("Không tìm thấy danh mục nào phù hợp");
        }

        // 2. Tạo Product
        LocalDateTime now = LocalDateTime.now();

        Product product = Product.builder()
                .supplier(supplierShop)
                .categories(categories)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .importPrice(request.getImportPrice())  // ⭐ thêm
                .quantity(request.getQuantity())
                .active(true)
                .soldQuantity(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        product = productRepository.save(product);

        // 3. Upload ảnh lên Cloudinary & lưu ProductImage
        List<String> imageUrls = new ArrayList<>();
        String mainImageUrl = null;
        boolean first = true;

        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) continue;

            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "market-hub/products/" + product.getId()
                        )
                );

                String url = (String) uploadResult.get("secure_url");
                String publicId = (String) uploadResult.get("public_id");

                if (url == null) {
                    throw new RuntimeException("Không lấy được URL ảnh từ Cloudinary");
                }

                imageUrls.add(url);

                ProductImage productImage = ProductImage.builder()
                        .product(product)
                        .imageUrl(url)
                        .publicId(publicId)
                        .mainImage(first)
                        .build();

                productImageRepository.save(productImage);

                if (first) {
                    mainImageUrl = url;
                    first = false;
                }

            } catch (IOException e) {
                throw new RuntimeException("Upload ảnh lên Cloudinary thất bại", e);
            }
        }

        product.setUpdatedAt(LocalDateTime.now());
        product = productRepository.save(product);

        return mapToResponse(product, imageUrls, mainImageUrl);
    }

    // =====================================
    //         MAP ENTITY -> RESPONSE
    // =====================================

    private ProductResponse mapToResponse(Product product, List<String> imageUrls, String mainImageUrl) {

        // Ảnh
        if (imageUrls == null || imageUrls.isEmpty()) {
            imageUrls = product.getImages()
                    .stream()
                    .map(ProductImage::getImageUrl)
                    .toList();
        }

        String thumbnailUrl = mainImageUrl;
        if (thumbnailUrl == null && !imageUrls.isEmpty()) {
            thumbnailUrl = imageUrls.get(0);
        }

        // Danh mục
        List<Category> categoryList = product.getCategories() != null
                ? product.getCategories()
                : Collections.emptyList();

        List<Long> categoryIds = categoryList.stream()
                .map(Category::getId)
                .toList();

        List<String> categoryNames = categoryList.stream()
                .map(Category::getName)
                .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .supplierId(product.getSupplier().getId())
                .categoryIds(categoryIds)
                .categoryNames(categoryNames)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .importPrice(product.getImportPrice())  // ⭐ thêm
                .quantity(product.getQuantity())
                .active(product.isActive())
                .thumbnailUrl(thumbnailUrl)
                .images(imageUrls)
                .soldQuantity(product.getSoldQuantity())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    // =====================================
    //          LIST MY PRODUCTS
    // =====================================

    /**
     * Nhà cung cấp xem danh sách sản phẩm của chính mình
     * (phân trang + search + lọc NHIỀU danh mục + khoảng giá)
     */
    public Page<ProductResponse> getMyProducts(
            int page,
            int size,
            String search,
            List<Long> categoryIds,
            Long minPrice,
            Long maxPrice
    ) {
        SupplierShop supplierShop = getCurrentSupplierShopOrThrow();

        // Chuẩn hoá tham số
        String keyword = (search == null || search.isBlank())
                ? null
                : search.trim();

        List<Long> filterCategoryIds =
                (categoryIds == null || categoryIds.isEmpty())
                        ? null
                        : categoryIds;

        Long min = (minPrice == null || minPrice <= 0) ? null : minPrice;
        Long max = (maxPrice == null || maxPrice <= 0) ? null : maxPrice;

        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // searchMyProducts: đã cập nhật query để join categories & lọc theo IN (:categoryIds)
        Page<Product> productPage = productRepository.searchMyProducts(
                supplierShop.getId(),
                keyword,
                filterCategoryIds,
                min,
                max,
                pageable
        );

        return productPage.map(p -> mapToResponse(p, null, null));
    }

    // =====================================
    //          GET MY PRODUCT DETAIL
    // =====================================

    /**
     * Nhà cung cấp xem chi tiết 1 sản phẩm của chính mình
     */
    public ProductResponse getMyProductDetail(Long productId) {
        SupplierShop supplierShop = getCurrentSupplierShopOrThrow();

        Product product = productRepository
                .findByIdAndSupplier_Id(productId, supplierShop.getId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        return mapToResponse(product, null, null);
    }

    // =====================================
    //          UPDATE PRODUCT
    // =====================================

    /**
     * Nhà cung cấp sửa thông tin sản phẩm
     * - request: name / description / price / quantity / categoryIds / importPrice
     * - newImages: ảnh mới upload thêm
     * - keepImages: danh sách URL ảnh cũ muốn giữ lại
     */
    public ProductResponse updateProduct(
            Long productId,
            ProductCreateRequest request,
            MultipartFile[] newImages,
            List<String> keepImages
    ) {
        SupplierShop supplierShop = getCurrentSupplierShopOrThrow();

        Product product = productRepository
                .findByIdAndSupplier_Id(productId, supplierShop.getId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // ------- Cập nhật thông tin cơ bản + danh mục -------
        if (request.getCategoryIds() == null || request.getCategoryIds().isEmpty()) {
            throw new RuntimeException("Phải chọn ít nhất 1 danh mục");
        }

        List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
        if (categories.isEmpty()) {
            throw new RuntimeException("Không tìm thấy danh mục nào phù hợp");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImportPrice(request.getImportPrice()); // ⭐ thêm
        product.setQuantity(request.getQuantity());
        product.setCategories(categories);
        product.setUpdatedAt(LocalDateTime.now());

        // ------------ Xử lý ảnh ------------
        if (keepImages == null) {
            keepImages = new ArrayList<>();
        }

        // 1. Ảnh cũ
        List<ProductImage> oldImages = productImageRepository.findByProduct_Id(productId);

        // Xoá những ảnh KHÔNG nằm trong keepImages
        for (ProductImage img : oldImages) {
            if (!keepImages.contains(img.getImageUrl())) {
                productImageRepository.delete(img);
            }
        }

        // 2. Upload ảnh mới
        List<String> finalUrls = new ArrayList<>(keepImages);
        String thumbnailUrl = keepImages.isEmpty() ? null : keepImages.get(0);

        if (newImages != null) {
            for (MultipartFile file : newImages) {
                if (file == null || file.isEmpty()) continue;

                try {
                    Map<?, ?> upload = cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "folder", "market-hub/products/" + productId
                            )
                    );

                    String url = (String) upload.get("secure_url");
                    if (url == null) {
                        throw new RuntimeException("Không lấy được URL ảnh từ Cloudinary");
                    }

                    finalUrls.add(url);

                    productImageRepository.save(
                            ProductImage.builder()
                                    .product(product)
                                    .imageUrl(url)
                                    .mainImage(false)
                                    .build()
                    );

                    if (thumbnailUrl == null) {
                        thumbnailUrl = url;
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Upload ảnh lên Cloudinary thất bại", e);
                }
            }
        }

        Product saved = productRepository.save(product);

        return mapToResponse(saved, finalUrls, thumbnailUrl);
    }

    // =====================================
    //          TOGGLE ACTIVE
    // =====================================

    /**
     * Bật / tắt trạng thái sản phẩm (đang bán <-> ngừng bán)
     */
    public ProductResponse toggleActive(Long productId) {
        SupplierShop supplierShop = getCurrentSupplierShopOrThrow();

        Product product = productRepository
                .findByIdAndSupplier_Id(productId, supplierShop.getId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        product.setActive(!product.isActive());
        product.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(product);

        return mapToResponse(saved, null, null);
    }

    // =====================================
    //          UPDATE PRODUCT IMAGES
    // =====================================

    /**
     * Cập nhật ảnh sản phẩm
     * - imageIdsToDelete: danh sách ID ảnh cần xoá
     * - newImages: ảnh mới upload thêm
     */
    public ProductResponse updateProductImages(
            Long productId,
            List<Long> imageIdsToDelete,
            MultipartFile[] newImages
    ) {
        SupplierShop supplierShop = getCurrentSupplierShopOrThrow();

        Product product = productRepository
                .findByIdAndSupplier_Id(productId, supplierShop.getId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // ==== 1. Lấy tất cả ảnh hiện tại ====
        List<ProductImage> oldImages = productImageRepository.findByProduct_Id(productId);

        if (imageIdsToDelete == null) {
            imageIdsToDelete = List.of();
        }

        List<String> finalUrls = new ArrayList<>();

        // ==== 2. Xoá ảnh được chọn ====
        for (ProductImage img : oldImages) {
            if (imageIdsToDelete.contains(img.getId())) {
                if (img.getPublicId() != null) {
                    try {
                        cloudinary.uploader().destroy(img.getPublicId(), ObjectUtils.emptyMap());
                    } catch (IOException e) {
                        throw new RuntimeException("Xoá ảnh trên Cloudinary thất bại", e);
                    }
                }
                productImageRepository.delete(img);
            } else {
                finalUrls.add(img.getImageUrl());
            }
        }

        // ==== 3. Upload ảnh mới ====
        String thumbnailUrl = finalUrls.isEmpty() ? null : finalUrls.get(0);

        if (newImages != null) {
            for (MultipartFile file : newImages) {
                if (file == null || file.isEmpty()) continue;

                try {
                    Map<?, ?> upload = cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "folder", "market-hub/products/" + productId
                            )
                    );

                    String url = (String) upload.get("secure_url");
                    String publicId = (String) upload.get("public_id");

                    if (url == null) {
                        throw new RuntimeException("Không lấy được URL ảnh từ Cloudinary");
                    }

                    finalUrls.add(url);

                    productImageRepository.save(
                            ProductImage.builder()
                                    .product(product)
                                    .imageUrl(url)
                                    .publicId(publicId)
                                    .mainImage(false)
                                    .build()
                    );

                    if (thumbnailUrl == null) {
                        thumbnailUrl = url;
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Upload ảnh lên Cloudinary thất bại", e);
                }
            }
        }

        product.setUpdatedAt(LocalDateTime.now());
        Product saved = productRepository.save(product);

        return mapToResponse(saved, finalUrls, thumbnailUrl);
    }

}
