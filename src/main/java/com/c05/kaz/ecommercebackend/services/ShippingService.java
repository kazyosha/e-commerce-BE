package com.c05.kaz.ecommercebackend.services;

import com.c05.kaz.ecommercebackend.dto.shipping.ShippingFeeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private final RestTemplate restTemplate;

    @Value("${ghn.token}")
    private String ghnToken;

    @Value("${ghn.shopId}")
    private String shopId;

    @Value("${ghn.fromDistrictId}")   // ⭐ thêm cấu hình kho của shop
    private Integer fromDistrictId;

    /**
     * Tính phí GHN chỉ dựa vào địa chỉ
     * Không yêu cầu FE gửi cân nặng → dùng weight mặc định
     */
    public JsonNode getAvailableServices(int fromDistrictId, int toDistrictId) {

        String url = "https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/available-services";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        headers.set("ShopId", shopId); // header vẫn cần
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectNode body = new ObjectMapper().createObjectNode();
        body.put("shop_id", Integer.parseInt(shopId));  // ⭐ BẮT BUỘC
        body.put("from_district", fromDistrictId);
        body.put("to_district", toDistrictId);

        HttpEntity<ObjectNode> entity = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        return response.getBody().get("data");
    }


    public JsonNode calculateFee(ShippingFeeRequest req) {

        String url = "https://online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);
        headers.set("ShopId", shopId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectNode body = new ObjectMapper().createObjectNode();

        body.put("from_district_id", req.getFromDistrictId());
        body.put("to_district_id", req.getToDistrictId());
        body.put("to_ward_code", req.getToWardCode());
        body.put("service_id", req.getServiceId());
        body.put("weight", req.getWeight());
        body.put("height", req.getHeight());
        body.put("width", req.getWidth());
        body.put("length", req.getLength());

        // ⭐ BẮT BUỘC
        body.put("shop_id", Integer.parseInt(shopId));

        HttpEntity<ObjectNode> entity = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        return response.getBody().get("data");
    }

}
