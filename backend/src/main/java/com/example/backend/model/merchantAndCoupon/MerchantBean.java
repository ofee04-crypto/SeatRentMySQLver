package com.example.backend.model.merchantAndCoupon;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant")
@Getter // 確保使用 Getter/Setter
@Setter
@NoArgsConstructor
public class MerchantBean implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchantId")
    private Integer merchantId;

    @Column(name = "merchantName")
    private String merchantName;

    @Column(name = "merchantPhone")
    private String merchantPhone;

    @Column(name = "merchantEmail")
    private String merchantEmail;

    @Column(name = "merchantAddress")
    private String merchantAddress;

    @Column(name = "merchantStatus")
    private int merchantStatus;

    @Column(name = "createdTime", insertable = false, updatable = false)
    private LocalDateTime createdTime;

    @Override
    public String toString() {
        return "MerchantBean [merchantId=" + merchantId + ", merchantName=" + merchantName + "]";
    }
}