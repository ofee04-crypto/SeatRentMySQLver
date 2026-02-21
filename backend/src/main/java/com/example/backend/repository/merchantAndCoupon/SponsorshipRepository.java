package com.example.backend.repository.merchantAndCoupon;

import java.util.List;
import java.util.Optional;
import com.example.backend.model.merchantAndCoupon.SponsorshipRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SponsorshipRepository extends JpaRepository<SponsorshipRecord, Integer> {
    // 透過訂單編號找紀錄，這是更新狀態時最核心的功能
    List<SponsorshipRecord> findAllByOrderBySponsorIdDesc();

    Optional<SponsorshipRecord> findByMerchantTradeNo(String merchantTradeNo);
}
