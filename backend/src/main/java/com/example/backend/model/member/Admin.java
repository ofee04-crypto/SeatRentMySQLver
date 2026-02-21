package com.example.backend.model.member;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admId")
    private Integer admId;

    @Column(name = "admUsername", nullable = false, unique = true)
    private String admUsername;

    @Column(name = "admPassword", nullable = false)
    private String admPassword;

    @Column(name = "admName", nullable = false)
    private String admName;

    @Column(name = "admEmail", nullable = false)
    private String admEmail;

    @Column(name = "admRole", nullable = false)
    private Integer admRole;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;

    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;

    // 在類別裡面補上這兩個自動設定，這樣你新增、修改就都不用手動 set 時間了
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Column(name = "admStatus")
    private Integer admStatus = 1;
}