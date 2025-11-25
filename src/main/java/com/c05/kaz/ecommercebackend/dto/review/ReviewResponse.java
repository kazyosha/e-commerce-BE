package com.c05.kaz.ecommercebackend.dto.review;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private int rating;
    private String comment;
    private List<String> images;
    private String customerName;
    private String avatar;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String reply;
    private LocalDateTime repliedAt;
}
