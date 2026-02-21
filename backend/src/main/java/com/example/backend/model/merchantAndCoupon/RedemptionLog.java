package com.example.backend.model.merchantAndCoupon;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "redemption_log")
@Data // 如果你沒用 Lombok，請手動產生 Getter/Setter
public class RedemptionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer logId;

    // 關連會員 ID
    @Column(name = "memId", nullable = false)
    private Integer memId;

    // 關連優惠券 ID
    @Column(name = "couponId", nullable = false)
    private Integer couponId;

    // 記錄當下扣了多少點 (預防以後優惠券改價錢，紀錄要維持當初的點數)
    @Column(name = "pointsSpent", nullable = false)
    private Integer pointsSpent;

    // 記錄核銷時的優惠券名稱 (存快照，避免原優惠券被刪除後找不到名稱)
    @Column(name = "couponName", length = 100)
    private String couponName;

    @CreationTimestamp
    @Column(name = "redeemTime", nullable = false, updatable = false)
    private LocalDateTime redeemTime;

    public RedemptionLog() {
    }

    // 方便用的建構子
    public RedemptionLog(Integer memId, Integer couponId, Integer pointsSpent, String couponName) {
        this.memId = memId;
        this.couponId = couponId;
        this.pointsSpent = pointsSpent;
        this.couponName = couponName;
    }
}