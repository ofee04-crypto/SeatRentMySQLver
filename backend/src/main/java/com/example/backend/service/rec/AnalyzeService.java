package com.example.backend.service.rec;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.repository.rec.RecRentAnalyzeRepository;
import com.example.backend.repository.spot.RentalSpotRepository;

@Service
public class AnalyzeService {

    @Autowired
    private RentalSpotRepository rentalSpotRepository;

    @Autowired
    private RecRentAnalyzeRepository recRentAnalyzeRepository;

    /**
     * 獲取儀表板所需的所有統計數據
     * 將不同 Repository 的查詢結果彙整到一個 Map 中
     */
    public Map<String, Object> getAnalyzeStats(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> data = new HashMap<>();

        // 縣市分佈 (來自 RentalSpotRepository)
        // 對應前端圖表：圓餅圖 (Pie Chart)
        data.put("cityDistribution", rentalSpotRepository.getCityDistribution());

        // 站點即時監控 (來自 RentalSpotRepository)
        // 對應前端圖表：列表或儀表板 (Gauge/Table)
        data.put("spotMonitor", rentalSpotRepository.getSpotRealtimeStatus());

        // 時段熱度 (來自 RecRentAnalyzeRepository)
        // 對應前端圖表：折線圖 (Line Chart)
        data.put("hourlyHeatMap", recRentAnalyzeRepository.getHourlyHeatMap(startDate, endDate));

        // 使用時間長短統計 (來自 RecRentAnalyzeRepository)
        // 對應前端圖表：長條圖 (Bar Chart)
        data.put("durationStats", recRentAnalyzeRepository.getDurationStats(startDate, endDate));

        // 熱門點位 (來自 RentalSpotRepository)
        // 用於首頁推薦
        data.put("hotSpots", rentalSpotRepository.getHotSpots());

        return data;
    }
}
