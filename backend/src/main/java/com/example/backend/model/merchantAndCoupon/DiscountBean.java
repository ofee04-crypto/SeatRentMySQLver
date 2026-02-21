package com.example.backend.model.merchantAndCoupon;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "discount")
@Getter // 改用 Getter/Setter，比 @Data 安全
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscountBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "couponId")
    private Integer couponId;

    @Column(name = "couponName")
    private String couponName;

    @Column(name = "couponDescription")
    private String couponDescription;

    @Column(name = "pointsRequired")
    private Integer pointsRequired;

    @Column(name = "startDate")
    private LocalDate startDate;

    @Column(name = "endDate")
    private LocalDate endDate;

    @Column(name = "merchantId")
    private Integer merchantId;

    @Column(name = "couponStatus")
    private Integer couponStatus;

    @Column(name = "couponImg")
    private String couponImg;

    @Column(name = "createdTime", insertable = false, updatable = false)
    private LocalDateTime createdTime;

    // 關聯商家 (加上 Lazy 載入，並防止序列化報錯)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchantId", insertable = false, updatable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private MerchantBean merchant;

    /**
     * 這裡不是資料庫欄位，但轉 JSON 時會自動產生 "merchantName" 屬性。
     * 前端 Vue 看到的 JSON 結構跟原本一模一樣，所以畫面不會壞。
     */
    @Transient
    public String getMerchantName() {
        return merchant != null ? merchant.getMerchantName() : null;
    }
}