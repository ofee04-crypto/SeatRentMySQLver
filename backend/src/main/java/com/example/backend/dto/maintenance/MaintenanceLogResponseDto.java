package com.example.backend.dto.maintenance;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 維修歷程回應 DTO (防止 Entity 洩漏)
 */
public class MaintenanceLogResponseDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Integer logId;
    private String operator;   // 操作者
    private String action;     // 動作代號 (CREATED, ASSIGNED, STARTED, RESOLVED, CANCELLED, URGENT)
    private String comment;    // 備註說明
    private String createdAt;  // 格式化後的時間字串 (yyyy-MM-dd HH:mm)

    // ========== Constructors ==========

    public MaintenanceLogResponseDto() {}

    /**
     * 從 Entity 轉換為 DTO
     */
    public MaintenanceLogResponseDto(Integer logId, String operator, String action, 
                                     String comment, LocalDateTime createdAt) {
        this.logId = logId;
        this.operator = operator;
        this.action = action;
        this.comment = comment;
        this.createdAt = (createdAt != null) ? createdAt.format(FORMATTER) : null;
    }

    // ========== Getters & Setters ==========

    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
