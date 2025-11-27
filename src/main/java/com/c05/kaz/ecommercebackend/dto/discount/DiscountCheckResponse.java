package com.c05.kaz.ecommercebackend.dto.discount;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiscountCheckResponse {

    private boolean valid;         // Mã dùng được hay không
    private Long discountAmount;   // Số tiền giảm (nếu dùng được)
    private String reason;         // Nếu invalid -> lý do
}
