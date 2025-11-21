package com.c05.kaz.ecommercebackend.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GhnAddressService {

    private final RestTemplate restTemplate;

    @Value("${ghn.token}")
    private String ghnToken;

    // ===============================
    // GHN API
    // ===============================

    public JsonNode getProvinces() {
        String url = "https://online-gateway.ghn.vn/shiip/public-api/master-data/province";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> res = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                JsonNode.class
        );

        return res.getBody().get("data");
    }

    public JsonNode getDistricts(Integer provinceId) {
        String url = "https://online-gateway.ghn.vn/shiip/public-api/master-data/district";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);

        HttpEntity<Map<String, Integer>> entity =
                new HttpEntity<>(Map.of("province_id", provinceId), headers);

        ResponseEntity<JsonNode> res = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        return res.getBody().get("data");
    }

    public JsonNode getWards(Integer districtId) {
        String url = "https://online-gateway.ghn.vn/shiip/public-api/master-data/ward";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnToken);

        HttpEntity<Map<String, Integer>> entity =
                new HttpEntity<>(Map.of("district_id", districtId), headers);

        ResponseEntity<JsonNode> res = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                JsonNode.class
        );

        return res.getBody().get("data");
    }

    // ===============================
    // GET NAMES
    // ===============================

    public String getProvinceName(Integer id) {
        if (id == null) return null;

        JsonNode list = getProvinces();
        for (JsonNode p : list) {
            if (p.get("ProvinceID").asInt() == id) {
                return p.get("ProvinceName").asText();
            }
        }
        return null;
    }

    public String getDistrictName(Integer id) {
        if (id == null) return null;

        // GHN có endpoint lấy toàn bộ district nếu truyền provinceId = 0
        JsonNode list = getDistricts(0);

        for (JsonNode d : list) {
            if (d.get("DistrictID").asInt() == id) {
                return d.get("DistrictName").asText();
            }
        }
        return null;
    }

    public String getWardName(String wardCode, Integer districtId) {
        if (wardCode == null || districtId == null) {
            return null;
        }

        JsonNode wards = getWards(districtId);

        if (wards == null || !wards.isArray()) {
            return null;
        }

        for (JsonNode w : wards) {
            if (w.get("WardCode").asText().equals(wardCode)) {
                return w.get("WardName").asText();
            }
        }

        return null;
    }
}
