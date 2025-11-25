package com.c05.kaz.ecommercebackend.enums;

public enum MessageStatus {
    SENT,        // server đã nhận
    DELIVERED,   // đã đẩy vào websocket
    READ         // người nhận đã đọc
}
