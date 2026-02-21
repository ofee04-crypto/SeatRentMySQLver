package com.example.backend.repository.rec;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.model.rec.RecRent;
import com.example.backend.repository.projection.AnalyzeProjections.DurationRate;
import com.example.backend.repository.projection.AnalyzeProjections.HourlyRate;

/**
 * 專門用於儀表板統計的 Repository
 */
@Repository
public interface RecRentAnalyzeRepository extends JpaRepository<RecRent, Integer> {

    // 需求 3: 每日每一時間之熱度 (全站總計)
    @Query(value = """
                SELECT
                    DATEPART(HOUR, recRentDT2) as hourofDay,
                    COUNT(*) as rentedCount
                FROM recRent
                WHERE (:startDate IS NULL OR recRentDT2 >= :startDate)
                  AND (:endDate IS NULL OR recRentDT2 < DATEADD(day, 1, :endDate))
                GROUP BY DATEPART(HOUR, recRentDT2)
                ORDER BY hourofDay
            """, nativeQuery = true)
    List<HourlyRate> getHourlyHeatMap(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 需求 4: 使用時間長短統計
    @Query(value = """
                SELECT
                    CASE
                        WHEN DATEDIFF(MINUTE, recRentDT2, recReturnDT2) <= 60 THEN '1小時內'
                        WHEN DATEDIFF(MINUTE, recRentDT2, recReturnDT2) <= 120 THEN '1-2小時'
                        WHEN DATEDIFF(MINUTE, recRentDT2, recReturnDT2) <= 240 THEN '2-4小時'
                        ELSE '4小時以上'
                    END as durationRange,
                    COUNT(*) as count
                FROM recRent
                WHERE recStatus = N'已完成' AND recReturnDT2 IS NOT NULL
                  AND (:startDate IS NULL OR recRentDT2 >= :startDate)
                  AND (:endDate IS NULL OR recRentDT2 < DATEADD(day, 1, :endDate))
                GROUP BY
                    CASE
                        WHEN DATEDIFF(MINUTE, recRentDT2, recReturnDT2) <= 60 THEN '1小時內'
                        WHEN DATEDIFF(MINUTE, recRentDT2, recReturnDT2) <= 120 THEN '1-2小時'
                        WHEN DATEDIFF(MINUTE, recRentDT2, recReturnDT2) <= 240 THEN '2-4小時'
                        ELSE '4小時以上'
                    END
            """, nativeQuery = true)
    List<DurationRate> getDurationStats(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}