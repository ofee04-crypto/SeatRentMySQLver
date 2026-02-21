package com.example.backend.controller.maintenance;

import com.example.backend.dto.maintenance.AssetStatsResponseDto;
import com.example.backend.service.maintenance.AssetStatsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 資產健康度統計 Controller
 * 提供機台(Spot)與椅子(Seat)的維修/保養統計 API
 */
@RestController
@RequestMapping("/api/maintenance/stats")
@CrossOrigin(origins = "http://localhost:5173")
public class AssetStatsController {

    private final AssetStatsService statsService;

    public AssetStatsController(AssetStatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * 取得資產健康度統計
     * GET /api/maintenance/stats/assets?assetType=SPOT|SEAT
     * 
     * @param assetType 資產類型：SPOT(機台) 或 SEAT(椅子)
     * @return 資產統計列表
     */
    @GetMapping("/assets")
    public ResponseEntity<?> getAssetStats(@RequestParam(required = false) String assetType) {
        // 參數驗證
        if (assetType == null || assetType.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "缺少必要參數 assetType，請提供 SPOT 或 SEAT"));
        }

        String type = assetType.trim().toUpperCase();

        switch (type) {
            case "SPOT":
                List<AssetStatsResponseDto> spotStats = statsService.getSpotStats();
                return ResponseEntity.ok(spotStats);

            case "SEAT":
                List<AssetStatsResponseDto> seatStats = statsService.getSeatStats();
                return ResponseEntity.ok(seatStats);

            default:
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "無效的 assetType: " + assetType + "，請提供 SPOT 或 SEAT"));
        }
    }

    /**
     * 例外處理
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "統計計算發生錯誤: " + ex.getMessage()));
    }
}
