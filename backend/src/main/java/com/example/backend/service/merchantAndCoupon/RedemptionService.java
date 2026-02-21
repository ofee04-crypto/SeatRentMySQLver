package com.example.backend.service.merchantAndCoupon;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.RedemptionLogDTO;
import com.example.backend.model.merchantAndCoupon.RedemptionLog;
import com.example.backend.repository.merchantAndCoupon.DiscountRepository;
import com.example.backend.repository.merchantAndCoupon.RedemptionLogRepository;

@Service
public class RedemptionService {
    @Autowired
    private RedemptionLogRepository redemptionLogRepository;

    // 假設你有 CouponRepository 來查商家資訊
    @Autowired
    private DiscountRepository discountRepository;

    @Transactional(readOnly = true)
    public List<RedemptionLogDTO> getMemberLogs(Integer memId) {
        List<RedemptionLog> logs = redemptionLogRepository.findByMemIdOrderByRedeemTimeDesc(memId);

        return logs.stream().map(log -> {
            // 透過 couponId 找出對應的商家名稱
            String merchantName = discountRepository.findById(log.getCouponId())
                    .map(d -> d.getMerchant().getMerchantName())
                    .orElse("未知商家");
            return new RedemptionLogDTO(
                    log.getLogId(),
                    log.getMemId(),
                    log.getCouponId(),
                    log.getPointsSpent(),
                    log.getCouponName(),
                    log.getRedeemTime(),
                    merchantName);
        }).collect(Collectors.toList());
    }
}
