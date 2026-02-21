package com.example.backend.dto.rec;

// 定義一個數據傳輸對象(DTO)，用於封裝每月訂單數量的統計結果
public class MonthlyOrderCountDTO {

    // 儲存年份
    private Integer year;
    // 儲存月份
    private Integer month;
    // 儲存該月的訂單總數
    private Long orderCount;

    // 建構子，用於JPQL查詢直接映射結果。使用包裝類型(Integer, Long)以匹配JPQL函數返回的類型。
    public MonthlyOrderCountDTO(Integer year, Integer month, Long orderCount) {
        this.year = year;
        this.month = month;
        this.orderCount = orderCount;
    }

    // Year 的 Getter
    public Integer getYear() {
        return year;
    }

    // Year 的 Setter
    public void setYear(Integer year) {
        this.year = year;
    }

    // Month 的 Getter
    public Integer getMonth() {
        return month;
    }

    // Month 的 Setter
    public void setMonth(Integer month) {
        this.month = month;
    }

    // OrderCount 的 Getter
    public Long getOrderCount() {
        return orderCount;
    }

    // OrderCount 的 Setter
    public void setOrderCount(Long orderCount) {
        this.orderCount = orderCount;
    }
}
