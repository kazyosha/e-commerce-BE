package com.c05.kaz.ecommercebackend.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class SepayPaymentForm {
    private String endpoint;
    private Map<String, String> fields;
}
