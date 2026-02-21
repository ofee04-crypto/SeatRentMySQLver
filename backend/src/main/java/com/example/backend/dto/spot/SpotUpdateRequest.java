package com.example.backend.dto.spot;

import java.math.BigDecimal;

import lombok.Data;

// DTO (Data Transfer Object): 一個簡單的 Java 物件，專門用來在各層之間傳遞資料。
// 這裡用來對應前端更新 Spot 時傳來的 JSON 物件。
@Data
public class SpotUpdateRequest {

    private Integer spotId;
    private String spotCode;
    private String spotName;
    private String spotAddress;
    private String spotStatus;
    private Integer merchantId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String spotDescription;
    private String spotImage;

}