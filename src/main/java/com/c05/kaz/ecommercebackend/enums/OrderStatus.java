package com.c05.kaz.ecommercebackend.enums;

public enum OrderStatus {
    PENDING,     // Chờ xác nhận
    REJECTED,    // Bị từ chối
    CANCELED,    // Đã hủy
    SHIPPING,    // Đang giao
    COMPLETED    // Hoàn tất / đã nhận hàng
}