package com.c05.kaz.ecommercebackend.services;


import com.c05.kaz.ecommercebackend.dto.payment.SepayPaymentForm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SepayService {

    @Value("${sepay.merchant-id}")
    private String merchantId;

    @Value("${sepay.secret-key}")
    private String secretKey;

    @Value("${sepay.checkout-endpoint}")
    private String endpoint;

    @Value("${sepay.success-url}")
    private String successUrl;

    @Value("${sepay.error-url}")
    private String errorUrl;

    @Value("${sepay.cancel-url}")
    private String cancelUrl;

    public SepayPaymentForm createPayment(long amount,
                                          String orderDescription,
                                          String invoiceNo,
                                          String customerId)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Map<String, String> fields = new HashMap<>();
        fields.put("merchant", merchantId);
        fields.put("currency", "VND");
        fields.put("operation", "PURCHASE");
        fields.put("payment_method", "BANK_TRANSFER");
        fields.put("order_amount", String.valueOf(amount));
        fields.put("order_description", orderDescription);
        fields.put("order_invoice_number", invoiceNo);
        fields.put("customer_id", customerId);
        fields.put("success_url", successUrl);
        fields.put("error_url", errorUrl);
        fields.put("cancel_url", cancelUrl);

        String signature = generateSignature(fields, secretKey);
        fields.put("signature", signature);

        return new SepayPaymentForm(endpoint, fields);
    }

    private String generateSignature(Map<String, String> fields, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {

        List<String> signKeys = Arrays.asList(
                "merchant",
                "operation",
                "payment_method",
                "order_amount",
                "currency",
                "order_invoice_number",
                "order_description",
                "customer_id",
                "success_url",
                "error_url",
                "cancel_url"
        );

        List<String> parts = new ArrayList<>();
        for (String key : signKeys) {
            parts.add(key + "=" + fields.getOrDefault(key, ""));
        }

        String signedString = String.join(",", parts);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(signedString.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(raw);
    }
}
