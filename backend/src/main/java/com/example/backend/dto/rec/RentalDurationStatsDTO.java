package com.example.backend.dto.rec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用於封裝租借時長統計數據的數據傳輸物件 (DTO)。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalDurationStatsDTO {
    // 租借時長的分組 (例如，以30分鐘為一個單位)
    private Integer intervalGroup;
    // 該分組內的訂單數量
    private Long count;
}
