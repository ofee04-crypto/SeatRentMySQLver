package com.example.backend.model.spot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Table(name = "renting_Spot")
@Data
public class RentalSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spotId")
    private Integer spotId;

    @NotBlank(message = "據點代碼不得為空")
    @Column(name = "spotCode", length = 30, nullable = false)
    private String spotCode;

    @NotBlank(message = "據點名稱不得為空")
    @Column(name = "spotName", length = 100, nullable = false)
    private String spotName;

    @Column(name = "spotAddress", length = 200)
    private String spotAddress;

    @NotBlank(message = "據點狀態不得為空")
    @Column(name = "spotStatus", length = 20, nullable = false)
    private String spotStatus;

    @Column(name = "merchantId")
    private Integer merchantId;

    // 因 DB 已有 Default 與 Trigger，改由 DB 全權管理，JPA 不寫入、只讀取
    @Column(name = "createdAt", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    // 因 DB 已有 Default 與 Trigger，改由 DB 全權管理，JPA 不寫入、只讀取
    @Column(name = "updatedAt", updatable = false, insertable = false)
    private LocalDateTime updatedAt;

    @Column(name = "latitude", precision = 10, scale = 7) // 10 總位數，7 小數位
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7) // 10 總位數，7 小數位
    private BigDecimal longitude;

    @Column(name = "spotDescription", length = 500)
    private String spotDescription;

    @Column(name = "spotImage", length = 255)
    private String spotImage;

    public RentalSpot() {
    }

    public RentalSpot(String spotCode, String spotName, String spotStatus) {
        this.spotCode = spotCode;
        this.spotName = spotName;
        this.spotStatus = spotStatus;
    }

    public RentalSpot(String spotCode, String spotName, String spotAddress, String spotStatus, Integer merchantId,
            BigDecimal latitude, BigDecimal longitude) {
        this.spotCode = spotCode;
        this.spotName = spotName;
        this.spotAddress = spotAddress;
        this.spotStatus = spotStatus;
        this.merchantId = merchantId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

}