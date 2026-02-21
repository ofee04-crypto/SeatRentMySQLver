package com.example.backend.dto.rec;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

// Lombok註解，自動生成getter, setter, toString, equals, hashCode
@Data
// 自動生成包含所有參數的建構函數
@AllArgsConstructor
// 自動生成無參數的建構函數
@NoArgsConstructor
public class OrderStatusStatsDTO {
    // 訂單狀態
    private String status;
    // 該狀態的訂單數量
    private long count;
}
