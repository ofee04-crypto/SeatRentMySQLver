package com.example.backend.model.maintenance;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 維修歷程記錄表 (Audit Log)
 * 記錄工單的每一次狀態變更、操作記錄
 */
@Entity
@Table(name = "maintenanceLog")
public class MaintenanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "logId")
    private Integer logId;

    /**
     * 關聯工單 (多對一)
     * 使用 @JsonBackReference 防止序列化時發生無限迴圈
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticketId", nullable = false)
    @JsonBackReference
    private MaintenanceInformation ticket;

    @Column(name = "operator", nullable = false, length = 50)
    private String operator; // 操作者 (支援中文)

    @Column(name = "action", nullable = false, length = 50)
    private String action; // 動作代號 (CREATED, ASSIGNED, STARTED, RESOLVED, CANCELLED)

    @Column(name = "comment", length = 500)
    private String comment; // 詳細說明 (可選)

    /**
     * 發生時間 (由 DB 自動產生)
     * insertable = false, updatable = false 確保應用層不會覆蓋 DB 預設值
     */
    @Column(name = "createdAt", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========== Constructors ==========

    public MaintenanceLog() {}

    /**
     * 快速建構子 (用於 Service 層建立 Log)
     */
    public MaintenanceLog(MaintenanceInformation ticket, String operator, String action, String comment) {
        this.ticket = ticket;
        this.operator = operator;
        this.action = action;
        this.comment = comment;
    }

    // ========== Getters & Setters ==========

    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    public MaintenanceInformation getTicket() {
        return ticket;
    }

    public void setTicket(MaintenanceInformation ticket) {
        this.ticket = ticket;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
