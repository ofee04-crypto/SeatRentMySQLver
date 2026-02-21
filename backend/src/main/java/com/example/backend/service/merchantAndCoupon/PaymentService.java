package com.example.backend.service.merchantAndCoupon;

import com.example.backend.model.merchantAndCoupon.SponsorshipRecord;
import com.example.backend.model.member.Member; // 💡 引入會員 Model
import com.example.backend.repository.merchantAndCoupon.SponsorshipRepository;
import com.example.backend.repository.member.MemberRepository; // 💡 引入會員 Repo
import com.example.backend.utils.EcpayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PaymentService {

    @Autowired
    private SponsorshipRepository sponsorshipRepository;

    @Autowired
    private MemberRepository memberRepository; // 💡 注入會員倉庫

    @Autowired
    private EcpayUtils ecpayUtils;

    /**
     * 1. 產製贊助表單並存入資料庫
     */
    public String createSponsorshipOrder(Integer memberId, BigDecimal amount, String comment, String baseUrl) {
        String merchantTradeNo = "SPN" + System.currentTimeMillis();

        SponsorshipRecord record = new SponsorshipRecord();
        record.setMemberId(memberId);
        record.setAmount(amount);
        record.setMerchantTradeNo(merchantTradeNo);
        record.setSponsorComment(comment);
        record.setStatus(0);
        sponsorshipRepository.save(record);

        String itemName = "贊助支持並獲取積分-" + amount;
        String amountStr = String.valueOf(amount.intValue());

        return ecpayUtils.genCheckOutForm(merchantTradeNo, amountStr, itemName, baseUrl);
    }

    /**
     * 2. 更新支付狀態 + 自動送積分 (贊助成功回呼)
     */
    @Transactional
    public void processPaymentResult(Map<String, String> formData) {
        System.out.println(">>> 進入加點流程，訂單編號: " + formData.get("MerchantTradeNo"));
        String merchantTradeNo = formData.get("MerchantTradeNo");
        String rtnCode = formData.get("RtnCode"); // "1" 代表成功

        sponsorshipRepository.findByMerchantTradeNo(merchantTradeNo).ifPresent(record -> {
            // 如果這筆紀錄原本是待支付(0)，且現在回傳成功(1)
            if ("1".equals(rtnCode) && record.getStatus() == 0) {

                // A. 更新贊助紀錄狀態
                record.setStatus(1); // 成功
                record.setPaymentType(formData.get("PaymentType"));
                record.setTradeNo(formData.get("TradeNo"));
                // sponsorshipRepository.save(record); // @Transactional 會自動處理

                // B. 💡 執行「贊助送積分」邏輯
                // 這裡我們比照你 GameController 的邏輯：
                // 贊助多少錢就送多少點 (1:1)，你也可以改成 record.getAmount() * 10

                int pointsToAdd = record.getAmount().intValue();

                memberRepository.findById(record.getMemberId()).ifPresent(member -> {
                    System.out.println(">>> 找到資料庫紀錄，目前狀態為: " + record.getStatus());
                    int currentPoints = member.getMemPoints();
                    member.setMemPoints(currentPoints + pointsToAdd);
                    memberRepository.save(member);
                    System.out.println("【系統自動加點】會員 ID: " + member.getMemId() + " 獲贈 " + pointsToAdd + " 點");
                });

            } else if (!"1".equals(rtnCode)) {
                record.setStatus(2); // 失敗
            }
        });
    }
}