package com.example.backend.repository.spot;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.backend.model.spot.RentalSpot;
import com.example.backend.repository.projection.AnalyzeProjections.SpotCountByCity;
import com.example.backend.repository.projection.AnalyzeProjections.SpotMonitor;
import com.example.backend.repository.projection.AnalyzeProjections.HotSpot;

@Repository
public interface RentalSpotRepository extends JpaRepository<RentalSpot, Integer>, JpaSpecificationExecutor<RentalSpot> {

    @Query("SELECT r FROM RentalSpot r WHERE r.spotCode LIKE %:keyword% OR r.spotName LIKE %:keyword% OR r.spotAddress LIKE %:keyword%")
    List<RentalSpot> findByKeyword(@Param("keyword") String keyword);

    boolean existsBySpotCode(String spotCode);

    @Query(value = """
                SELECT
                    SUBSTRING(spotAddress, 1, 3) as city,
                    COUNT(*) as spotCount
                FROM renting_Spot
                WHERE spotAddress IS NOT NULL
                GROUP BY SUBSTRING(spotAddress, 1, 3)
                ORDER BY spotCount DESC
            """, nativeQuery = true)
    List<SpotCountByCity> getCityDistribution();

    @Query(value = """
                SELECT
                    s.spotId   AS spotId,
                    s.spotName AS spotName,
                    20 AS totalSeats,
                    COALESCE(curr.availableCount, 0) AS availableSeats
                FROM renting_Spot s
                LEFT JOIN (
                    SELECT spotId, COUNT(*) AS availableCount
                    FROM seats
                    WHERE seatsStatus = N'啟用' AND spotId IS NOT NULL
                    -- 排除目前正在租借中的座位
                    AND seatsId NOT IN (
                        SELECT CAST(seatsId AS INT) FROM recRent WHERE recStatus = N'租借中'
                    )
                    GROUP BY spotId
                ) curr ON curr.spotId = s.spotId
                WHERE s.spotStatus = N'營運中'
            """, nativeQuery = true)
    List<SpotMonitor> getSpotRealtimeStatus();

    /**
     * 獲取熱門點位 (依據租借次數排序，取前 4 名)
     * 用於首頁 HomeView 呈現。
     */
    @Query(value = """
                SELECT TOP 4
                    s.spotId,
                    s.spotName,
                    s.spotStatus,
                    COALESCE(curr.availableCount, 0) AS availableSeats,
                    s.spotImage,
                    s.latitude,
                    s.longitude,
                    COUNT(r.recId) AS orderCount
                FROM renting_Spot s
                LEFT JOIN (
                    SELECT spotId, COUNT(*) AS availableCount
                    FROM seats
                    WHERE seatsStatus = N'啟用' AND spotId IS NOT NULL
                    -- 排除目前正在租借中的座位
                    AND seatsId NOT IN (
                        SELECT CAST(seatsId AS INT) FROM recRent WHERE recStatus = N'租借中'
                    )
                    GROUP BY spotId
                ) curr ON curr.spotId = s.spotId
                LEFT JOIN recRent r ON r.spotIdRent = s.spotId
                WHERE s.spotStatus = N'營運中'
                GROUP BY s.spotId, s.spotName, s.spotStatus, curr.availableCount, s.spotImage, s.latitude, s.longitude
                ORDER BY orderCount DESC
            """, nativeQuery = true)
    List<HotSpot> getHotSpots();

    /**
     * [新增] 獲取所有營運中的站點及其即時座位數
     * 用於首頁地圖標記呈現。
     */
    @Query(value = """
                SELECT
                    s.spotId,
                    s.spotName,
                    s.spotStatus,
                    s.latitude,
                    s.longitude,
                    s.spotImage,
                    COALESCE(curr.availableCount, 0) AS availableSeats
                FROM renting_Spot s
                LEFT JOIN (
                    SELECT spotId, COUNT(*) AS availableCount
                    FROM seats
                    WHERE seatsStatus = N'啟用' AND spotId IS NOT NULL
                    AND seatsId NOT IN (
                        SELECT CAST(seatsId AS INT) FROM recRent WHERE recStatus = N'租借中'
                    )
                    GROUP BY spotId
                ) curr ON curr.spotId = s.spotId
                WHERE s.spotStatus = N'營運中'
            """, nativeQuery = true)
    List<com.example.backend.repository.projection.AnalyzeProjections.SpotWithSeats> findAllSpotsWithSeatCount();
}