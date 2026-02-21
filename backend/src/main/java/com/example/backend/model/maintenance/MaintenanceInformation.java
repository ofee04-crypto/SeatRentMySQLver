package com.example.backend.model.maintenance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Table(name = "maintenanceInformation")
public class MaintenanceInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticketId")
    private Integer ticketId;

    @Column(name = "spotId")
    private Integer spotId;

    //新增對應資料庫 seatsId 欄位
    @Column(name = "seatsId")
    private Integer seatsId;

    @Column(name = "issueType", nullable = false, length = 200)
    private String issueType;

    @Column(name = "issueDesc", length = 500)
    private String issueDesc;

    @Column(name = "issuePriority", nullable = false, length = 100)
    private String issuePriority;

    @Column(name = "issueStatus", nullable = false, length = 50)
    private String issueStatus;

    @Column(name = "assignedStaffId")
    private Integer assignedStaffId;

    // ★ 修正：insertable=false, updatable=false 代表這欄位只負責「讀資料」，寫入還是靠上面的 assignedStaffId
    // ★ 修正：加上 @JsonIgnoreProperties 防止 JSON 序列化時無限迴圈
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignedStaffId", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    @JsonIgnoreProperties({"maintenanceInformations", "hibernateLazyInitializer", "handler"})
    private MaintenanceStaff assignedStaff;

    // ================== Task 4: 新增站點與座位關聯 ==================
    
    // 關聯到據點資料，用於顯示站點名稱
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "spotId", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private com.example.backend.model.spot.RentalSpot rentalSpot;

    // 關聯到座位資料，用於顯示座位名稱
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seatsId", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private com.example.backend.model.spot.Seat seat;




    // DB DEFAULT SYSDATETIME()：建議由 DB 產生
    @Column(name = "reportedAt", insertable = false, updatable = false)
    private LocalDateTime reportedAt;

    @Column(name = "startAt")
    private LocalDateTime startAt;

    @Column(name = "resolvedAt")
    private LocalDateTime resolvedAt;

    @Column(name = "resolveNote", length = 500)
    private String resolveNote;

    @Column(name = "resultType", length = 50)
    private String resultType;

    public MaintenanceInformation() {}

    // getters/setters
    public Integer getTicketId() { return ticketId; }
    public void setTicketId(Integer ticketId) { this.ticketId = ticketId; }

    public Integer getSpotId() { return spotId; }
    public void setSpotId(Integer spotId) { this.spotId = spotId; }

    public Integer getSeatsId() { return seatsId; }
    public void setSeatsId(Integer seatsId) { this.seatsId = seatsId;}

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getIssueDesc() { return issueDesc; }
    public void setIssueDesc(String issueDesc) { this.issueDesc = issueDesc; }

    public String getIssuePriority() { return issuePriority; }
    public void setIssuePriority(String issuePriority) { this.issuePriority = issuePriority; }

    public String getIssueStatus() { return issueStatus; }
    public void setIssueStatus(String issueStatus) { this.issueStatus = issueStatus; }

    public Integer getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(Integer assignedStaffId) { this.assignedStaffId = assignedStaffId; }

    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }

    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolveNote() { return resolveNote; }
    public void setResolveNote(String resolveNote) { this.resolveNote = resolveNote; }

    public String getResultType() { return resultType; }
    public void setResultType(String resultType) { this.resultType = resultType; }


    // Getter & Setter
    public MaintenanceStaff getAssignedStaff() {
        return assignedStaff;
    }

    public void setAssignedStaff(MaintenanceStaff assignedStaff) {
        this.assignedStaff = assignedStaff;
    }

    // ================== Task 4: 新增關聯的 Getter/Setter ==================
    
    public com.example.backend.model.spot.RentalSpot getRentalSpot() {
        return rentalSpot;
    }

    public void setRentalSpot(com.example.backend.model.spot.RentalSpot rentalSpot) {
        this.rentalSpot = rentalSpot;
    }

    public com.example.backend.model.spot.Seat getSeat() {
        return seat;
    }

    public void setSeat(com.example.backend.model.spot.Seat seat) {
        this.seat = seat;
    }
}
