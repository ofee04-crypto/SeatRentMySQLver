package com.example.backend.dto.maintenance;

import java.time.LocalDateTime;

public class MaintenanceStaffResponseDto {

    private Integer staffId;
    private String staffName;
    private String staffCompany;
    private String staffEmail;
    private String staffPhone;
    private String staffNote;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public MaintenanceStaffResponseDto() {}

    public MaintenanceStaffResponseDto(Integer staffId, String staffName, String staffCompany,
                                       String staffEmail, String staffPhone, String staffNote,
                                       Boolean isActive, LocalDateTime createdAt) {
        this.staffId = staffId;
        this.staffName = staffName;
        this.staffCompany = staffCompany;
        this.staffEmail = staffEmail;
        this.staffPhone = staffPhone;
        this.staffNote = staffNote;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public Integer getStaffId() { return staffId; }
    public void setStaffId(Integer staffId) { this.staffId = staffId; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getStaffCompany() { return staffCompany; }
    public void setStaffCompany(String staffCompany) { this.staffCompany = staffCompany; }

    public String getStaffEmail() { return staffEmail; }
    public void setStaffEmail(String staffEmail) { this.staffEmail = staffEmail; }

    public String getStaffPhone() { return staffPhone; }
    public void setStaffPhone(String staffPhone) { this.staffPhone = staffPhone; }

    public String getStaffNote() { return staffNote; }
    public void setStaffNote(String staffNote) { this.staffNote = staffNote; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
