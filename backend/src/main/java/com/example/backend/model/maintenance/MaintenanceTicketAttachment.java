package com.example.backend.model.maintenance;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 維修工單附件 Entity
 * 對應資料表 maintenanceTicketAttachment
 */
@Entity
@Table(name = "maintenanceTicketAttachment")
public class MaintenanceTicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachmentId")
    private Integer attachmentId;

    /**
     * 關聯工單 (多對一)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticketId", nullable = false)
    @JsonBackReference
    private MaintenanceInformation ticket;

    @Column(name = "ticketId", insertable = false, updatable = false)
    private Integer ticketId;

    @Column(name = "originalName", nullable = false, length = 255)
    private String originalName;

    @Column(name = "storedName", nullable = false, length = 255)
    private String storedName;

    @Column(name = "contentType", nullable = false, length = 100)
    private String contentType;

    @Column(name = "fileSize", nullable = false)
    private Long fileSize;

    @Column(name = "publicUrl", nullable = false, length = 400)
    private String publicUrl;

    @Column(name = "sortOrder", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "note", length = 200)
    private String note;

    /**
     * 建立時間（由 DB 自動產生）
     */
    @Column(name = "createdAt", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "isActive", nullable = false)
    private Boolean isActive = true;

    // ========== Constructors ==========

    public MaintenanceTicketAttachment() {}

    // ========== Getters & Setters ==========

    public Integer getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Integer attachmentId) {
        this.attachmentId = attachmentId;
    }

    public MaintenanceInformation getTicket() {
        return ticket;
    }

    public void setTicket(MaintenanceInformation ticket) {
        this.ticket = ticket;
    }

    public Integer getTicketId() {
        return ticketId;
    }

    public void setTicketId(Integer ticketId) {
        this.ticketId = ticketId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
