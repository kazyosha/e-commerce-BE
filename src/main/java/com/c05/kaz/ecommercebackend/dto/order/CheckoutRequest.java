package com.c05.kaz.ecommercebackend.dto.order;

import com.c05.kaz.ecommercebackend.enums.PaymentMethod;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {

    private List<CheckoutItemRequest> items;

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    private Long shippingFee;
    private String discountCode;


    private PaymentMethod paymentMethod; // COD hoặc BANK_GATEWAY
}
