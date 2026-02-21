package com.example.backend.controller.rec;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.service.rec.AnalyzeService;

@RestController
@RequestMapping("/api/analyze")
public class AnalyzeController {

    @Autowired
    private AnalyzeService analyzeService;

    /**
     * 取得儀表板統計數據
     * API: GET /api/analyze/stats
     * 回傳: JSON 物件，包含 cityDistribution, spotMonitor, hourlyHeatMap, durationStats,
     * hotSpots
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAnalyzeStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> stats = analyzeService.getAnalyzeStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /**
     * 取得站點即時監控數據（專供調度中心中心使用）
     * API: GET /api/analyze/spot-monitor
     */
    @GetMapping("/spot-monitor")
    public ResponseEntity<Object> getSpotMonitor() {
        Map<String, Object> stats = analyzeService.getAnalyzeStats(null, null);
        return ResponseEntity.ok(stats.get("spotMonitor"));
    }

    /**
     * 取得熱門點位數據（專供首頁使用）
     * API: GET /api/analyze/hot-spots
     */
    @GetMapping("/hot-spots")
    public ResponseEntity<Object> getHotSpots() {
        Map<String, Object> stats = analyzeService.getAnalyzeStats(null, null);
        return ResponseEntity.ok(stats.get("hotSpots"));
    }
}