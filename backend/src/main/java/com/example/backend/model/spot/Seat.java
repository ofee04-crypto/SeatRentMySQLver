package com.example.backend.model.spot;

import java.time.LocalDateTime;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "seats")
@Data
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seatsId")
    private Integer seatsId;

    @NotBlank(message = "座位名稱不得為空")
    @Column(name = "seatsName", length = 100, nullable = false)
    private String seatsName;

    @NotBlank(message = "座位類型不得為空")
    @Column(name = "seatsType", length = 50, nullable = false)
    private String seatsType;

    @NotBlank(message = "座位狀態不得為空")
    @Column(name = "seatsStatus", length = 20, nullable = false)
    private String seatsStatus;

    @Column(name = "spotId")
    private Integer spotId;

    @Column(name = "serialNumber", length = 50, insertable = false, updatable = false)
    private String serialNumber;

    // [優化] 因 DB 已有 Default 與 Trigger，改由 DB 全權管理，JPA 不寫入、只讀取
    @Column(name = "createdAt", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    // [優化] 因 DB 已有 Default 與 Trigger，改由 DB 全權管理，JPA 不寫入、只讀取
    @Column(name = "updatedAt", updatable = false, insertable = false)
    private LocalDateTime updatedAt;

    public Seat() {
    }

    public Seat(Integer seatsId, String seatsName, String seatsType, String seatsStatus,
            Integer spotId, LocalDateTime updatedAt, String serialNumber, LocalDateTime createdAt) {
        this.seatsId = seatsId;
        this.seatsName = seatsName;
        this.seatsType = seatsType;
        this.seatsStatus = seatsStatus;
        this.spotId = spotId;
        this.updatedAt = updatedAt;
        this.serialNumber = serialNumber;
        this.createdAt = createdAt;
    }

}
