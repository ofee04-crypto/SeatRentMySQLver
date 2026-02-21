package com.example.backend.model.rec;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Immutable // 標記為唯讀，因為這是 View
@Table(name = "V_RentDetails")
public class RentDetails {

    @Id
    @Column(name = "recId")
    private String recId;

    @Column(name = "memId")
    private Integer memId;

    @Column(name = "memName")
    private String memName;

    @Column(name = "couponId")
    private Integer couponId;

    @Column(name = "couponDesc")
    private String couponDesc;

    @Column(name = "seatsId")
    private String seatsId;

    @Column(name = "spotIdRent")
    private Integer spotIdRent;

    @Column(name = "RentSpotName")
    private String rentSpotName;

    @Column(name = "spotIdReturn")
    private Integer spotIdReturn;

    @Column(name = "ReturnSpotName")
    private String returnSpotName;

    @Column(name = "recRentDT2")
    private LocalDateTime recRentDT2;

    @Column(name = "recReturnDT2")
    private LocalDateTime recReturnDT2;

    @Column(name = "recUsageDT2")
    private Integer recUsageDT2;

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

    @Column(name = "recViolatInt")
    private Integer recViolatInt;

    @Column(name = "recNote")
    private String recNote;

    @Column(name = "recStatus")
    private String recStatus;

}
