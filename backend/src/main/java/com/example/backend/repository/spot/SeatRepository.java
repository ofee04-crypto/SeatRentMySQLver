package com.example.backend.repository.spot;

import java.util.List; // 確認這裡是 java.util.List，絕對不能是 java.awt.List

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.model.spot.Seat;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer>, JpaSpecificationExecutor<Seat> {

    // 補上 Service 層呼叫的模糊查詢方法
    @Query("SELECT s FROM Seat s WHERE s.seatsName LIKE %:keyword% OR s.serialNumber LIKE %:keyword% OR s.seatsType LIKE %:keyword%")
    List<Seat> findByKeyword(@Param("keyword") String keyword);

    /**
     * 檢查指定的 serialNumber 是否已存在於資料庫中。
     * 
     * @param serialNumber 要檢查的序號
     * @return 如果存在則返回 true，否則返回 false。
     */
    boolean existsBySerialNumber(String serialNumber);

    // 根據據點 ID 查詢該據點下的所有座位
    List<Seat> findBySpotId(Integer spotId);

    // 根據據點 ID 查詢該據點下的座位數量
    long countBySpotId(Integer spotId);

    // 查詢符合前綴的最後一筆座位資料 (用於自動產生序號，例如找 SN-2026 開頭的最大值)
    Seat findTopBySerialNumberStartingWithOrderBySerialNumberDesc(String prefix);
}