package com.c05.kaz.ecommercebackend.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateRequest {

    @NotNull(message = "Danh sách danh mục không được để trống")
    @Size(min = 1, message = "Phải chọn ít nhất 1 danh mục")
    private List<Long> categoryIds;   // ✔️ CHỌN NHIỀU CATEGORY

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm tối đa 255 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @Min(value = 1, message = "Giá phải lớn hơn 0")
    private Long price; // VND

    @NotNull(message = "Giá nhập không được để trống")
    @Min(value = 0, message = "Giá nhập không được âm")
    private Long importPrice; // ⭐ thêm mới

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
}
