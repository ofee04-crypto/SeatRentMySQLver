package com.example.backend.dto.rec;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

// Lombok註解，自動生成getter, setter, toString, equals, hashCode
@Data
// 自動生成包含所有參數的建構函數
@AllArgsConstructor
// 自動生成無參數的建構函數
@NoArgsConstructor
public class RentalDurationDTO {
    // 訂單的唯一識別碼
    private String recId;
    // 租借開始時間
    private LocalDateTime rentTime;
    // 租借總時長（分鐘）
    private long durationInMinutes;
}
