package com.example.backend.model.member;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memId")
    private int memId;

    @Column(name = "memUsername", nullable = false, length = 50)
    private String memUsername;

    @Column(name = "memPassword", nullable = false, length = 100)
    private String memPassword;

    @Column(name = "memName", nullable = false, length = 50)
    private String memName;

    @Column(name = "memEmail", nullable = false, length = 50)
    private String memEmail;

    @Column(name = "memPhone", nullable = false, length = 20)
    private String memPhone;

    @Column(name = "memStatus", nullable = false)
    private int memStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "memPoints", nullable = false)
    private int memPoints;

    @Column(name = "memViolation", nullable = false)
    private int memViolation;

    @Column(name = "memLevel", nullable = false)
    private int memLevel;

    @Column(name = "memInvoice", length = 20)
    private String memInvoice;

    @Column(name = "memImage", length = 255)
    private String memImage = "default.png";

    public Member() {
    }

    // getter / setter
    public int getMemId() {
        return memId;
    }

    public void setMemId(int memId) {
        this.memId = memId;
    }

    public String getMemUsername() {
        return memUsername;
    }

    public void setMemUsername(String memUsername) {
        this.memUsername = memUsername;
    }

    public String getMemPassword() {
        return memPassword;
    }

    public void setMemPassword(String memPassword) {
        this.memPassword = memPassword;
    }

    public String getMemName() {
        return memName;
    }

    public void setMemName(String memName) {
        this.memName = memName;
    }

    public String getMemEmail() {
        return memEmail;
    }

    public void setMemEmail(String memEmail) {
        this.memEmail = memEmail;
    }

    public String getMemPhone() {
        return memPhone;
    }

    public void setMemPhone(String memPhone) {
        this.memPhone = memPhone;
    }

    public int getMemStatus() {
        return memStatus;
    }

    public void setMemStatus(int memStatus) {
        this.memStatus = memStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getMemPoints() {
        return memPoints;
    }

    public void setMemPoints(int memPoints) {
        this.memPoints = memPoints;
    }

    public int getMemViolation() {
        return memViolation;
    }

    public void setMemViolation(int memViolation) {
        this.memViolation = memViolation;
    }

    public int getMemLevel() {
        return memLevel;
    }

    public void setMemLevel(int memLevel) {
        this.memLevel = memLevel;
    }

    public String getMemInvoice() {
        return memInvoice;
    }

    public void setMemInvoice(String memInvoice) {
        this.memInvoice = memInvoice;
    }

    public String getMemImage() {
        return memImage;
    }

    public void setMemImage(String memImage) {
        this.memImage = memImage;
    }

}