package com.example.backend.model.maintenance;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 定期維護排程 Entity
 * 對應資料表 maintenanceSchedule
 */
@Data
@Entity
@Table(name = "maintenanceSchedule")
public class MaintenanceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scheduleId")
    private Integer scheduleId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 目標類型：'SPOT' 或 'SEAT'
     */
    @Column(name = "targetType", nullable = false, length = 20)
    private String targetType;

    /**
     * 目標 ID（對應 spotId 或 seatsId）
     */
    @Column(name = "targetId", nullable = false)
    private Integer targetId;

    /**
     * 排程類型：'DAILY', 'WEEKLY', 'MONTHLY'
     */
    @Column(name = "scheduleType", nullable = false, length = 20)
    private String scheduleType;

    /**
     * 星期幾 (1-7)，週排程時使用
     */
    @Column(name = "dayOfWeek")
    private Integer dayOfWeek;

    /**
     * 每月幾號 (1-31)，月排程時使用
     */
    @Column(name = "dayOfMonth")
    private Integer dayOfMonth;

    /**
     * 執行時間 (時:分:秒)
     */
    @Column(name = "executeTime", nullable = false)
    private LocalTime executeTime;

    /**
     * 問題類型
     */
    @Column(name = "issueType", nullable = false, length = 200)
    private String issueType;

    /**
     * 問題優先級：'LOW', 'NORMAL', 'HIGH', 'URGENT'
     */
    @Column(name = "issuePriority", nullable = false, length = 50)
    private String issuePriority;

    /**
     * 指派的維護人員 ID
     */
    @Column(name = "assignedStaffId")
    private Integer assignedStaffId;

    /**
     * 是否啟用
     */
    @Column(name = "isActive", nullable = false)
    private Boolean isActive;

    /**
     * 上次執行時間
     */
    @Column(name = "lastExecutedAt")
    private LocalDateTime lastExecutedAt;

    /**
     * 下次預計執行時間
     */
    @Column(name = "nextExecuteAt", nullable = false)
    private LocalDateTime nextExecuteAt;

    /**
     * 建立時間（由 DB 自動產生）
     */
    @Column(name = "createdAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新時間（由 DB 自動產生）
     */
    @Column(name = "updatedAt", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
