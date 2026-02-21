package com.example.backend.dto.spot;

import lombok.Data;

// DTO: 專門用來接收前端 "更新座位" 請求的資料載體
@Data
public class SeatUpdateRequest {

    private Integer seatsId;
    private String seatsName;
    private String seatsType;
    private String seatsStatus;
    private Integer spotId;
    private String serialNumber;

}