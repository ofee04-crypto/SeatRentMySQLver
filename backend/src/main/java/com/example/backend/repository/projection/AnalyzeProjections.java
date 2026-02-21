package com.example.backend.repository.projection;

public class AnalyzeProjections {

    // 站點於各城市之分布統計
    public interface SpotCountByCity {
        String getCity();

        Integer getSpotCount();
    }

    // 各時段租借次數統計
    public interface HourlyRate {
        Integer getHourofDay();

        Integer getRentedCount();
    }

    // 站點即時監控 (名稱、總座位、已租借)
    public interface SpotMonitor {
        Integer getSpotId(); // 新增 ID 以便前端進行調度操作

        String getSpotName();

        Integer getTotalSeats();

        Integer getAvailableSeats();
    }

    // 各租借時長區間統計
    public interface DurationRate {
        String getDurationRange();

        Integer getCount();
    }

    // 熱門點位統計 (首頁使用)
    public interface HotSpot {
        Integer getSpotId();

        String getSpotName();

        String getSpotStatus();

        Integer getAvailableSeats();

        String getSpotImage();

        Double getLatitude();

        Double getLongitude();

        Integer getOrderCount();
    }

    // [新增] 站點與即時座位數 (地圖標記使用)
    public interface SpotWithSeats {
        Integer getSpotId();

        String getSpotName();

        String getSpotStatus();

        Double getLatitude();

        Double getLongitude();

        String getSpotImage();

        Integer getAvailableSeats();
    }
}
