package com.example.backend.repository.rec;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.model.rec.RentDetails;

public interface RentDetailsRepository
                extends JpaRepository<RentDetails, String>, JpaSpecificationExecutor<RentDetails> {

        // JPQL查詢，用於按月統計指定時間範圍內的訂單數量
        // 返回Object[]而不是DTO，以避免JPA實現中的構造函數類型匹配問題
        @Query("SELECT FUNCTION('YEAR', r.recRentDT2), FUNCTION('MONTH', r.recRentDT2), COUNT(r) " +
                        "FROM RentDetails r " +
                        "WHERE r.recRentDT2 BETWEEN :startDate AND :endDate " +
                        "GROUP BY FUNCTION('YEAR', r.recRentDT2), FUNCTION('MONTH', r.recRentDT2) " +
                        "ORDER BY FUNCTION('YEAR', r.recRentDT2), FUNCTION('MONTH', r.recRentDT2)")
        List<Object[]> findMonthlyOrderCounts(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // JPQL查詢，用於統計指定時間範圍內各訂單狀態的數量（圓餅圖數據）
        @Query("SELECT r.recStatus, COUNT(r) FROM RentDetails r " +
                        "WHERE r.recRentDT2 BETWEEN :startDate AND :endDate " +
                        "GROUP BY r.recStatus")
        List<Object[]> findOrderStatusStats(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // JPQL查詢，用於獲取指定時間範圍內訂單的租借時間信息（散佈圖數據）
        // 篩選掉recReturnDT2為null的紀錄，因為無法計算時長
        @Query("SELECT r.recId, r.recRentDT2, r.recReturnDT2 FROM RentDetails r " +
                        "WHERE r.recRentDT2 BETWEEN :startDate AND :endDate AND r.recReturnDT2 IS NOT NULL")
        List<Object[]> findRentalDurations(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // 查詢：按小時統計訂單數量 (長條圖) - SQL Server 版本
        @Query(value = "SELECT HOUR(recRentDT2), COUNT(*) " +
                        "FROM V_RentDetails " + // 修正：使用正確的 View 與欄位名稱
                        "WHERE recRentDT2 BETWEEN :startDate AND :endDate " +
                        "GROUP BY HOUR(recRentDT2) " +
                        "ORDER BY HOUR(recRentDT2)", nativeQuery = true)
        List<Object[]> findHourlyOrderCounts(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // 查詢：按日統計訂單數量 (折線圖) - SQL Server 版本
        @Query(value = "SELECT DATE(recRentDT2), COUNT(*) " +
                        "FROM V_RentDetails " + // 修正：使用正確的 View 與欄位名稱
                        "WHERE recRentDT2 BETWEEN :startDate AND :endDate " +
                        "GROUP BY DATE(recRentDT2) " +
                        "ORDER BY DATE(recRentDT2)", nativeQuery = true)
        List<Object[]> findDailyOrderCounts(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // 查詢：按30分鐘間隔統計租借時長 - SQL Server 版本
        @Query(value = "SELECT floor(TIMESTAMPDIFF(MINUTE, recRentDT2, recReturnDT2) / 30) AS interval_group, COUNT(*) "
                        +
                        "FROM V_RentDetails " + // 修正：使用正確的 View 與欄位名稱
                        "WHERE recRentDT2 BETWEEN :startDate AND :endDate AND recReturnDT2 IS NOT NULL " +
                        "GROUP BY floor(TIMESTAMPDIFF(MINUTE, recRentDT2, recReturnDT2) / 30) " +
                        "ORDER BY interval_group", nativeQuery = true)
        List<Object[]> findRentalDurationStatsIn30MinIntervals(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);
}
