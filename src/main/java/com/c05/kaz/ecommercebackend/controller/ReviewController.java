package com.c05.kaz.ecommercebackend.controller;

import com.c05.kaz.ecommercebackend.dto.review.ReviewRequest;
import com.c05.kaz.ecommercebackend.services.ReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/{productId}")
    public ResponseEntity<?> getReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviews(productId));
    }

    @GetMapping("/{productId}/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyReview(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getMyReview(productId));
    }

    @PostMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createReview(
            @PathVariable Long productId,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "comment", required = false) String comment,
            @RequestPart(value = "images", required = false) MultipartFile[] images
    ) throws JsonProcessingException {

        return ResponseEntity.ok(
                reviewService.createReview(productId, rating, comment, images)
        );
    }

    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            @RequestPart(required = false) MultipartFile[] newImages,
            @RequestParam(required = false) List<String> keepImages // ảnh cũ giữ lại
    ) throws JsonProcessingException {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, rating, comment, newImages, keepImages));
    }

    @GetMapping("/{productId}/can")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> canReview(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.canReview(productId));
    }

    @PostMapping("/{reviewId}/reply")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<?> replyReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> body
    ) {
        String reply = body.get("reply");
        return ResponseEntity.ok(reviewService.replyReview(reviewId, reply));
    }

    @PutMapping("/{reviewId}/reply")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<?> updateReply(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(reviewService.updateReply(reviewId, body.get("reply")));
    }
}