package com.example.backend.dto.maintenance;

public class SpotOptionDto {
    private Integer spotId;
    private String spotCode;
    private String spotName;
    private String spotAddress;
    private String spotStatus;

    //  經緯度欄位（前端地圖用）
    // 建議用 Double（JSON 友善、前端也好處理）
    private Double latitude;
    private Double longitude;

    public SpotOptionDto() {}

    //  舊建構子：不影響既有程式（例如下拉選單只需要基本欄位）
    public SpotOptionDto(Integer spotId, String spotCode, String spotName, String spotAddress, String spotStatus) {
        this.spotId = spotId;
        this.spotCode = spotCode;
        this.spotName = spotName;
        this.spotAddress = spotAddress;
        this.spotStatus = spotStatus;
    }

    //  新建構子：給「需要地圖座標」的情境使用
    public SpotOptionDto(
            Integer spotId,
            String spotCode,
            String spotName,
            String spotAddress,
            String spotStatus,
            Double latitude,
            Double longitude
    ) {
        this.spotId = spotId;
        this.spotCode = spotCode;
        this.spotName = spotName;
        this.spotAddress = spotAddress;
        this.spotStatus = spotStatus;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Integer getSpotId() { return spotId; }
    public void setSpotId(Integer spotId) { this.spotId = spotId; }

    public String getSpotCode() { return spotCode; }
    public void setSpotCode(String spotCode) { this.spotCode = spotCode; }

    public String getSpotName() { return spotName; }
    public void setSpotName(String spotName) { this.spotName = spotName; }

    public String getSpotAddress() { return spotAddress; }
    public void setSpotAddress(String spotAddress) { this.spotAddress = spotAddress; }

    public String getSpotStatus() { return spotStatus; }
    public void setSpotStatus(String spotStatus) { this.spotStatus = spotStatus; }

    //  getter / setter：前端地圖讀 allSpots.value[n].latitude / longitude
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
