package com.example.backend.model.merchantAndCoupon;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "SponsorshipRecord")
@Data
public class SponsorshipRecord {
    @Column(name = "sponsorid")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sponsorId;

    @Column(name = "memberid", nullable = false)
    private Integer memberId;

    @Column(name = "merchanttradeno", nullable = false, unique = true, length = 50)
    private String merchantTradeNo; // 我們自己產生的編號

    @Column(name = "tradeno", length = 50)
    private String tradeNo; // 綠界回傳的編號

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "sponsorcomment", length = 500)
    private String sponsorComment; // 贊助者的留言

    /**
     * 0: 待支付 (Pending)
     * 1: 支付成功 (Success)
     * 2: 支付失敗 (Failed)
     */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "paymenttype")
    private String paymentType;

    @CreationTimestamp
    @Column(name = "createdat", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updatedat")
    private LocalDateTime updatedAt;
}