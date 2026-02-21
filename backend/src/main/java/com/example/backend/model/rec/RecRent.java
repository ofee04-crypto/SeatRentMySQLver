package com.example.backend.model.rec;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data // Lombok 自動產生 Getter, Setter, toString 等
@Entity
@Table(name = "recRent")
public class RecRent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 對應 SQL 的 IDENTITY(1,1)
    @Column(name = "recSeqId")
    private Integer recSeqId;

    // 對應 SQL 的計算欄位 (AS ... PERSISTED)，Java 端設為唯讀
    @Column(name = "recId", insertable = false, updatable = false)
    private String recId;

    @Column(name = "memId", nullable = false)
    private Integer memId;

    @Column(name = "couponId")
    private Integer couponId;

    @Column(name = "seatsId", nullable = false)
    private String seatsId;

    @Column(name = "spotIdRent", nullable = false)
    private Integer spotIdRent;

    @Column(name = "spotIdReturn")
    private Integer spotIdReturn;

    @Column(name = "recRentDT2", nullable = false)
    private LocalDateTime recRentDT2;

    @Column(name = "recReturnDT2")
    private LocalDateTime recReturnDT2;

    // 對應 SQL 的 DECIMAL
    @Column(name = "recUsageDT2")
    private BigDecimal recUsageDT2;

    @Column(name = "recStatus")
    private String recStatus;

    @Column(name = "recPrice")
    private Integer recPrice;

    @Column(name = "recRequestPay")
    private Integer recRequestPay;

    @Column(name = "recPayment")
    private Integer recPayment;

    @Column(name = "recPayBy")
    private String recPayBy;

    @Column(name = "recInvoice")
    private String recInvoice;

    @Column(name = "recCarrier")
    private String recCarrier;

    @Column(name = "recViolatInt", nullable = false)
    private Integer recViolatInt;

    @Column(name = "recNote")
    private String recNote;
}