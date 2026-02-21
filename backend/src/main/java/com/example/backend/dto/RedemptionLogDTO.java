package com.example.backend.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedemptionLogDTO {
    private Integer logId;
    private Integer memId;
    private Integer couponId;
    private Integer pointsSpent;
    private String couponName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime redeemTime;
    private String merchantName; // 這是前端要的欄位
}