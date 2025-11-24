package com.c05.kaz.ecommercebackend.dto.review;

import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {
    private int rating;
    private String comment;
    private List<String> images;  // các URL ảnh đã upload cloudinary
}
