package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.config.VnPayConfig;
import com.c05.kaz.ecommercebackend.dto.payment.VnPayCreateRequest;
import com.c05.kaz.ecommercebackend.dto.payment.VnPayCreateResponse;
import com.c05.kaz.ecommercebackend.entity.Order;
import com.c05.kaz.ecommercebackend.enums.OrderStatus;
import com.c05.kaz.ecommercebackend.enums.PaymentMethod;
import com.c05.kaz.ecommercebackend.exception.NotFoundException;
import com.c05.kaz.ecommercebackend.repository.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final VnPayConfig vnPayConfig;
    private final OrderRepository orderRepository;

    // ========== TẠO URL THANH TOÁN VNPAY ==========
    public VnPayCreateResponse createVnPayPayment(VnPayCreateRequest req, HttpServletRequest servletRequest) {
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order không tồn tại"));

        if (order.isPaid()) {
            throw new RuntimeException("Đơn hàng đã được thanh toán.");
        }

        if (order.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new RuntimeException("Đơn hàng này không sử dụng phương thức VNPay.");
        }

        long amount = req.getAmount() != null ? req.getAmount() : order.getFinalTotal();
        if (amount <= 0) {
            throw new RuntimeException("Số tiền không hợp lệ.");
        }

        String vnp_TxnRef = String.valueOf(order.getId()); // dùng id order làm mã giao dịch
        String vnp_OrderInfo = "Thanh toan don hang #" + order.getId();
        String vnp_IpAddr = getIpAddress(servletRequest);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay yêu cầu *100
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrCode());
        vnpParams.put("vnp_TxnRef", vnp_TxnRef);
        vnpParams.put("vnp_OrderInfo", vnp_OrderInfo);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", vnp_IpAddr);

        // thời gian tạo + hết hạn
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String createDate = now.format(fmt);
        String expireDate = now.plusMinutes(15).format(fmt);

        vnpParams.put("vnp_CreateDate", createDate);
        vnpParams.put("vnp_ExpireDate", expireDate);

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName)
                        .append('=')
                        .append(encode(fieldValue));

                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (i < fieldNames.size() - 1) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }

        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        String paymentUrl = vnPayConfig.getPayUrl() + "?" + query;

        return VnPayCreateResponse.builder()
                .paymentUrl(paymentUrl)
                .build();
    }

    // ========== HANDLE RETURN URL ==========
    @Transactional
    public String handleVnPayReturn(HttpServletRequest request) {
        Map<String, String> vnpParams = new HashMap<>();
        Map<String, String[]> fields = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : fields.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue()[0];
            if (key.startsWith("vnp_")) {
                vnpParams.put(key, value);
            }
        }

        String vnp_SecureHash = vnpParams.remove("vnp_SecureHash");
        // Nếu dùng vnp_SecureHashType thì cũng remove luôn
        vnpParams.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnpParams.get(fieldName);

            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append("=").append(fieldValue);
                if (i < fieldNames.size() - 1) {
                    hashData.append("&");
                }
            }
        }

        String signValue = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());

        if (!signValue.equals(vnp_SecureHash)) {
            // chữ ký không hợp lệ
            return "INVALID_SIGNATURE";
        }

        String responseCode = vnpParams.get("vnp_ResponseCode"); // 00 = success
        String txnRef = vnpParams.get("vnp_TxnRef");
        String transactionNo = vnpParams.get("vnp_TransactionNo");

        Long orderId = Long.parseLong(txnRef);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order không tồn tại"));

        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            order.setPaid(true);
            order.setPaymentTransactionId(transactionNo);
            order.setStatus(OrderStatus.COMPLETED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            return "SUCCESS";
        } else {
            // thất bại / bị hủy
            order.setPaid(false);
            order.setStatus(OrderStatus.CANCELED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            return "FAILED";
        }
    }

    // ========== HELPER ==========
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0];
        }
        return ip;
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.US_ASCII.toString());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac hmac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hash.append('0');
                hash.append(hex);
            }
            return hash.toString();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo chữ ký HMAC", e);
        }
    }
}
