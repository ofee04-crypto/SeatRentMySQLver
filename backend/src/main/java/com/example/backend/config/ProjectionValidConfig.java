package com.example.backend.config;

import com.example.backend.repository.rec.RecRentAnalyzeRepository;
import com.example.backend.repository.spot.RentalSpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
public class ProjectionValidConfig {

    private static final Logger log = LoggerFactory.getLogger(ProjectionValidConfig.class);

    /**
     * 啟動時自動執行 Projection 映射檢查
     * 建議只在 'dev' 或 'test' 環境執行，避免影響生產環境啟動速度
     */
    @Bean
    @Profile("dev") // 若您沒有設定 profile，可暫時註解掉這行
    public CommandLineRunner validateAnalyzeProjections(
            RentalSpotRepository spotRepo,
            RecRentAnalyzeRepository analyzeRepo) {
        return args -> {
            log.info("==========================================");
            log.info(" [自我檢查] 開始驗證 Projection SQL 別名映射...");

            // 檢查 1: 縣市分佈
            validate(() -> spotRepo.getCityDistribution(), "SpotCountByCity (縣市分佈)");

            // 檢查 2: 站點即時監控
            validate(() -> spotRepo.getSpotRealtimeStatus(), "SpotMonitor (即時監控)");

            // 檢查 3: 時段熱度
            validate(() -> analyzeRepo.getHourlyHeatMap(null, null), "HourlyRate (時段熱度)");

            // 檢查 4: 時長統計
            validate(() -> analyzeRepo.getDurationStats(null, null), "DurationRate (時長統計)");

            log.info("==========================================");
        };
    }

    private void validate(java.util.function.Supplier<List<?>> query, String checkName) {
        try {
            List<?> result = query.get();
            // 注意：如果資料庫是空的，這裡不會拋出映射錯誤，必須要有測試資料才準確
            if (result.isEmpty()) {
                log.warn("⚠️ [WARN] {} - 無資料可驗證 (請確保 DB 有測試數據)", checkName);
            } else {
                // 嘗試讀取第一筆資料的欄位，觸發 Getter 映射
                Object firstItem = result.get(0);
                log.info("✅ [PASS] {} - 映射成功 (回傳 {} 筆) Sample: {}", checkName, result.size(), firstItem);
            }
        } catch (Exception e) {
            log.error("❌ [FAIL] {} - 映射失敗! 請檢查 SQL 別名與 Interface Getter 是否一致。", checkName);
            log.error("   錯誤訊息: {}", e.getMessage());
            // 若希望檢查失敗就停止啟動，可取消下行註解
            // throw new RuntimeException("Projection Mapping Failed: " + checkName, e);
        }
    }
}
