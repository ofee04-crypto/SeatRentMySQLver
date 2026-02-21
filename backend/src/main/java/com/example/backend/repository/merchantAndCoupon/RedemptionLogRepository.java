package com.example.backend.repository.merchantAndCoupon;

import com.example.backend.dto.RedemptionLogDTO;
import com.example.backend.model.merchantAndCoupon.RedemptionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RedemptionLogRepository extends JpaRepository<RedemptionLog, Integer> {
    // 之後可以用來查詢某位會員的兌換紀錄
    List<RedemptionLog> findByMemIdOrderByRedeemTimeDesc(Integer memId);

    List<RedemptionLog> findAllByOrderByRedeemTimeDesc();
}