package com.example.backend.model.maintenance;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenanceStaff")
public class MaintenanceStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staffId")
    private Integer staffId;

    @Column(name = "staffName", nullable = false, length = 50)
    private String staffName;

    @Column(name = "staffCompany", length = 100)
    private String staffCompany;

    @Column(name = "staffPhone", length = 20)
    private String staffPhone;

    @Column(name = "staffEmail", length = 100)
    private String staffEmail;

    @Column(name = "staffNote", length = 200)
    private String staffNote;

    // DB 有 DEFAULT SYSDATETIME()，建議由 DB 產生
    @Column(name = "createdAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // 你後來 ADD 的欄位：isActive default 1
    @Column(name = "isActive", nullable = false)
    private Boolean isActive;
    

    public MaintenanceStaff() {}

    // getters/setters
    public Integer getStaffId() { return staffId; }
    public void setStaffId(Integer staffId) { this.staffId = staffId; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getStaffCompany() { return staffCompany; }
    public void setStaffCompany(String staffCompany) { this.staffCompany = staffCompany; }

    public String getStaffPhone() { return staffPhone; }
    public void setStaffPhone(String staffPhone) { this.staffPhone = staffPhone; }

    public String getStaffEmail() { return staffEmail; }
    public void setStaffEmail(String staffEmail) { this.staffEmail = staffEmail; }

    public String getStaffNote() { return staffNote; }
    public void setStaffNote(String staffNote) { this.staffNote = staffNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
