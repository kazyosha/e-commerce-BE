package com.c05.kaz.ecommercebackend.dto.shipping;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ShippingFeeRequest {

    @JsonProperty("from_district_id")
    private Integer fromDistrictId;

    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    @JsonProperty("service_id")
    private Integer serviceId;

    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
}
