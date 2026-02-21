package com.example.backend.dto.maintenance;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 排程回應 DTO (含人員姓名)
 */
public class MaintenanceScheduleResponseDto {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private Integer scheduleId;
    private String title;
    private String targetType;
    private Integer targetId;
    private String scheduleType;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private String executeTime;
    private String issueType;
    private String issuePriority;
    private Integer assignedStaffId;
    private String assignedStaffName;
    private Boolean isActive;
    private String lastExecutedAt;
    private String nextExecuteAt;
    private String createdAt;
    private String updatedAt;

    // ========== Constructors ==========

    public MaintenanceScheduleResponseDto() {}

    /**
     * 從 Entity 轉換的完整建構子 (含人員姓名)
     */
    public MaintenanceScheduleResponseDto(
            Integer scheduleId,
            String title,
            String targetType,
            Integer targetId,
            String scheduleType,
            Integer dayOfWeek,
            Integer dayOfMonth,
            LocalTime executeTime,
            String issueType,
            String issuePriority,
            Integer assignedStaffId,
            String assignedStaffName,
            Boolean isActive,
            LocalDateTime lastExecutedAt,
            LocalDateTime nextExecuteAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.scheduleId = scheduleId;
        this.title = title;
        this.targetType = targetType;
        this.targetId = targetId;
        this.scheduleType = scheduleType;
        this.dayOfWeek = dayOfWeek;
        this.dayOfMonth = dayOfMonth;
        this.executeTime = (executeTime != null) ? executeTime.format(TIME_FORMATTER) : null;
        this.issueType = issueType;
        this.issuePriority = issuePriority;
        this.assignedStaffId = assignedStaffId;
        this.assignedStaffName = assignedStaffName;
        this.isActive = isActive;
        this.lastExecutedAt = (lastExecutedAt != null) ? lastExecutedAt.format(DATE_FORMATTER) : null;
        this.nextExecuteAt = (nextExecuteAt != null) ? nextExecuteAt.format(DATE_FORMATTER) : null;
        this.createdAt = (createdAt != null) ? createdAt.format(DATE_FORMATTER) : null;
        this.updatedAt = (updatedAt != null) ? updatedAt.format(DATE_FORMATTER) : null;
    }

    // ========== Getters & Setters ==========

    public Integer getScheduleId() { return scheduleId; }
    public void setScheduleId(Integer scheduleId) { this.scheduleId = scheduleId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public String getExecuteTime() { return executeTime; }
    public void setExecuteTime(String executeTime) { this.executeTime = executeTime; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getIssuePriority() { return issuePriority; }
    public void setIssuePriority(String issuePriority) { this.issuePriority = issuePriority; }

    public Integer getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(Integer assignedStaffId) { this.assignedStaffId = assignedStaffId; }

    public String getAssignedStaffName() { return assignedStaffName; }
    public void setAssignedStaffName(String assignedStaffName) { this.assignedStaffName = assignedStaffName; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getLastExecutedAt() { return lastExecutedAt; }
    public void setLastExecutedAt(String lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }

    public String getNextExecuteAt() { return nextExecuteAt; }
    public void setNextExecuteAt(String nextExecuteAt) { this.nextExecuteAt = nextExecuteAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
