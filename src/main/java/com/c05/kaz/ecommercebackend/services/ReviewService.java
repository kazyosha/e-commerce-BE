package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.review.ReviewResponse;
import com.c05.kaz.ecommercebackend.entity.*;
import com.c05.kaz.ecommercebackend.enums.UserType;
import com.c05.kaz.ecommercebackend.repository.*;
import com.c05.kaz.ecommercebackend.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepo;
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepo;
    private final CloudinaryService uploadService;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepo;
    private final NotificationService notificationService;

    // Lấy danh sách đánh giá
    public List<ReviewResponse> getReviews(Long productId) {
        return reviewRepo.findByProduct_IdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // Lấy đánh giá của chính user (FE dùng check "đã đánh giá chưa")
    public ReviewResponse getMyReview(Long productId) {

        Long customerId = securityUtils.getCurrentUserId();

        return reviewRepo.findByProduct_IdAndCustomer_Id(productId, customerId)
                .map(this::toDto)
                .orElse(null);
    }

    // Tạo đánh giá
    @Transactional
    public ReviewResponse createReview(
            Long productId,
            Integer rating,
            String comment,
            MultipartFile[] images
    ) throws JsonProcessingException {

        Long customerId = securityUtils.getCurrentUserId();

        // 1 KH chỉ review 1 lần
        if (reviewRepo.findByProduct_IdAndCustomer_Id(productId, customerId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã đánh giá sản phẩm này");
        }
        boolean purchased = orderRepo.hasPurchasedProduct(customerId, productId);

        if (!purchased) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn chưa mua sản phẩm này nên không thể đánh giá"
            );
        }
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));

        CustomerProfile customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        // Upload ảnh
        List<String> uploadedImages = new ArrayList<>();
        if (images != null && images.length > 0) {
            uploadedImages = uploadService.uploadFiles(images);
        }

        Review review = Review.builder()
                .product(product)
                .customer(customer)
                .supplier(product.getSupplier())
                .rating(rating)
                .comment(comment)
                .images(objectMapper.writeValueAsString(uploadedImages))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        reviewRepo.save(review);
        notificationService.notifyReviewCreatedForSupplier(
                review.getProduct().getSupplier(),
                review.getCustomer().getUser(),
                productId,
                review.getId()
        );

        return toDto(review);
    }

    // Convert entity → DTO
    public ReviewResponse toDto(Review r) {

        List<String> imgs = new ArrayList<>();

        try {
            if (r.getImages() != null && !r.getImages().isBlank()) {
                imgs = objectMapper.readValue(
                        r.getImages(),
                        new TypeReference<List<String>>() {}
                );
            }
        } catch (Exception e) {
            imgs = new ArrayList<>();
        }

        return ReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .images(imgs)        // luôn trả mảng rỗng nếu không có ảnh
                .customerName(r.getCustomer().getFullName())
                .avatar(r.getCustomer().getAvatarUrl())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .reply(r.getReply())
                .repliedAt(r.getRepliedAt())
                .build();
    }

    @Transactional
    public ReviewResponse updateReview(
            Long reviewId,
            Integer rating,
            String comment,
            MultipartFile[] newImages,
            List<String> keepImages
    ) throws JsonProcessingException {

        Long customerId = securityUtils.getCurrentUserId();

        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!review.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền sửa đánh giá");
        }

        // Ảnh giữ lại
        List<String> finalImages = keepImages != null ? new ArrayList<>(keepImages) : new ArrayList<>();

        // Upload thêm ảnh mới nếu có
        if (newImages != null && newImages.length > 0) {
            List<String> uploaded = uploadService.uploadFiles(newImages);
            finalImages.addAll(uploaded);
        }

        review.setRating(rating);
        review.setComment(comment);
        review.setImages(objectMapper.writeValueAsString(finalImages));
        review.setUpdatedAt(LocalDateTime.now());

        reviewRepo.save(review);
        return toDto(review);
    }

    public boolean canReview(Long productId) {
        Long customerId = securityUtils.getCurrentUserId();
        return orderRepo.hasPurchasedProduct(customerId, productId);
    }

    @Transactional
    public ReviewResponse replyReview(Long reviewId, String reply) {

        Long userId = securityUtils.getCurrentUserId();

        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá"));

        // Chỉ nhà cung cấp shop đó mới được phản hồi
        Long supplierOwnerId = review.getProduct().getSupplier().getUser().getId();
        if (!supplierOwnerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền phản hồi đánh giá này");
        }

        review.setReply(reply);
        review.setRepliedAt(LocalDateTime.now());

        reviewRepo.save(review);
        notificationService.notifyReviewRepliedForCustomer(
                review.getCustomer().getUser(),
                review.getProduct().getId(),
                review.getId()
        );
        return toDto(review);
    }

    @Transactional
    public ReviewResponse updateReply(Long reviewId, String reply) {

        Long userId = securityUtils.getCurrentUserId();

        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá"));

        // Chỉ nhà cung cấp có quyền
        Long ownerId = review.getProduct().getSupplier().getUser().getId();
        if (!ownerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền sửa phản hồi này");
        }

        review.setReply(reply);
        review.setRepliedAt(LocalDateTime.now()); // cập nhật lại thời gian phản hồi

        reviewRepo.save(review);

        return toDto(review);
    }

}
