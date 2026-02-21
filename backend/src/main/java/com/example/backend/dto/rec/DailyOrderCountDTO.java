package com.example.backend.dto.rec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用於封裝每日訂單計數的數據傳輸物件 (DTO)。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyOrderCountDTO {
    // 日期 (格式: YYYY-MM-DD)
    private String date;
    // 該日期的訂單數量
    private Long count;
}
