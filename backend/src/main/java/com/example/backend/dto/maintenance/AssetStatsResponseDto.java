package com.example.backend.dto.maintenance;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 資產健康度統計 DTO
 * 用於回傳機台(Spot)或椅子(Seat)在特定時間窗內的維修/保養統計指標
 */
public class AssetStatsResponseDto {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String assetType;          // "SPOT" 或 "SEAT"
    private Integer assetId;           // spotId 或 seatsId
    private String assetName;          // 資產名稱

    private Integer repairCount;       // 維修工單數（非保養）
    private Integer maintainCount;     // 保養工單數
    private Integer repairDoneCount;   // 維修已結案數
    private Integer maintainDoneCount; // 保養已結案數
    private Integer openCount;         // 未結案數

    private Long downtimeMinutes;      // 停機分鐘數（時間窗內）
    private Double availability;       // 妥善率 (0~1)
    private Double failureRatePerDay;  // 故障率 (repairCount / 7)
    private Double repairRate;         // 維修率 (repairDoneCount / repairCount)

    private String windowFrom;         // 統計窗口起始時間
    private String windowTo;           // 統計窗口結束時間

    // ========== Constructors ==========

    public AssetStatsResponseDto() {}

    public AssetStatsResponseDto(
            String assetType,
            Integer assetId,
            String assetName,
            Integer repairCount,
            Integer maintainCount,
            Integer repairDoneCount,
            Integer maintainDoneCount,
            Integer openCount,
            Long downtimeMinutes,
            Double availability,
            Double failureRatePerDay,
            Double repairRate,
            LocalDateTime windowFrom,
            LocalDateTime windowTo
    ) {
        this.assetType = assetType;
        this.assetId = assetId;
        this.assetName = assetName;
        this.repairCount = repairCount;
        this.maintainCount = maintainCount;
        this.repairDoneCount = repairDoneCount;
        this.maintainDoneCount = maintainDoneCount;
        this.openCount = openCount;
        this.downtimeMinutes = downtimeMinutes;
        this.availability = availability;
        this.failureRatePerDay = failureRatePerDay;
        this.repairRate = repairRate;
        this.windowFrom = (windowFrom != null) ? windowFrom.format(DATE_FORMATTER) : null;
        this.windowTo = (windowTo != null) ? windowTo.format(DATE_FORMATTER) : null;
    }

    // ========== Getters & Setters ==========

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public Integer getAssetId() { return assetId; }
    public void setAssetId(Integer assetId) { this.assetId = assetId; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public Integer getRepairCount() { return repairCount; }
    public void setRepairCount(Integer repairCount) { this.repairCount = repairCount; }

    public Integer getMaintainCount() { return maintainCount; }
    public void setMaintainCount(Integer maintainCount) { this.maintainCount = maintainCount; }

    public Integer getRepairDoneCount() { return repairDoneCount; }
    public void setRepairDoneCount(Integer repairDoneCount) { this.repairDoneCount = repairDoneCount; }

    public Integer getMaintainDoneCount() { return maintainDoneCount; }
    public void setMaintainDoneCount(Integer maintainDoneCount) { this.maintainDoneCount = maintainDoneCount; }

    public Integer getOpenCount() { return openCount; }
    public void setOpenCount(Integer openCount) { this.openCount = openCount; }

    public Long getDowntimeMinutes() { return downtimeMinutes; }
    public void setDowntimeMinutes(Long downtimeMinutes) { this.downtimeMinutes = downtimeMinutes; }

    public Double getAvailability() { return availability; }
    public void setAvailability(Double availability) { this.availability = availability; }

    public Double getFailureRatePerDay() { return failureRatePerDay; }
    public void setFailureRatePerDay(Double failureRatePerDay) { this.failureRatePerDay = failureRatePerDay; }

    public Double getRepairRate() { return repairRate; }
    public void setRepairRate(Double repairRate) { this.repairRate = repairRate; }

    public String getWindowFrom() { return windowFrom; }
    public void setWindowFrom(String windowFrom) { this.windowFrom = windowFrom; }

    public String getWindowTo() { return windowTo; }
    public void setWindowTo(String windowTo) { this.windowTo = windowTo; }
}
