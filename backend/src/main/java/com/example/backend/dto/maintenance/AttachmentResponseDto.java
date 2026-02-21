package com.example.backend.dto.maintenance;

import com.example.backend.model.maintenance.MaintenanceTicketAttachment;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 附件回應 DTO
 */
public class AttachmentResponseDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Integer attachmentId;
    private Integer ticketId;
    private String originalName;
    private String storedName;
    private String contentType;
    private Long fileSize;
    private String publicUrl;
    private Integer sortOrder;
    private String note;
    private String createdAt;

    // ========== Constructors ==========

    public AttachmentResponseDto() {}

    /**
     * 從 Entity 轉換為 DTO
     */
    public AttachmentResponseDto(MaintenanceTicketAttachment entity) {
        this.attachmentId = entity.getAttachmentId();
        this.ticketId = entity.getTicketId();
        this.originalName = entity.getOriginalName();
        this.storedName = entity.getStoredName();
        this.contentType = entity.getContentType();
        this.fileSize = entity.getFileSize();
        this.publicUrl = entity.getPublicUrl();
        this.sortOrder = entity.getSortOrder();
        this.note = entity.getNote();
        this.createdAt = entity.getCreatedAt() != null ? entity.getCreatedAt().format(FORMATTER) : null;
    }

    // ========== Getters & Setters ==========

    public Integer getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Integer attachmentId) {
        this.attachmentId = attachmentId;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
