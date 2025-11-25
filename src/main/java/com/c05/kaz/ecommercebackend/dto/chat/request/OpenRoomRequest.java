package com.c05.kaz.ecommercebackend.dto.chat.request;
import lombok.Data;

@Data
public class OpenRoomRequest {
    private Long customerId;
    private Long supplierId;
    private Long orderId;
}
