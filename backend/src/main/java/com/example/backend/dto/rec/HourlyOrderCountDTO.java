package com.example.backend.dto.rec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用於封裝每小時訂單計數的數據傳輸物件 (DTO)。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HourlyOrderCountDTO {
    // 小時 (0-23)
    private Integer hour;
    // 該小時內的訂單數量
    private Long count;
}
