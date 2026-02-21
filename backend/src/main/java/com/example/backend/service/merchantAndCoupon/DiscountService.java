package com.example.backend.service.merchantAndCoupon;

import com.example.backend.model.merchantAndCoupon.DiscountBean;
import com.example.backend.model.merchantAndCoupon.RedemptionLog;
import com.example.backend.repository.merchantAndCoupon.DiscountRepository;
import com.example.backend.repository.merchantAndCoupon.RedemptionLogRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DiscountService {

    @Autowired
    private DiscountRepository discountRepository;

    @Autowired
    private RedemptionLogRepository redemptionLogRepository;

    public List<RedemptionLog> getAllRedemptionLogs() {
        // 依時間倒序排列，讓管理員先看到最新的紀錄
        return redemptionLogRepository.findAllByOrderByRedeemTimeDesc();
    }

    public List<DiscountBean> getAll(String keyword) {
        discountRepository.autoUpdateStatus();
        if (keyword != null && !keyword.trim().isEmpty()) {
            return discountRepository.findByKeyword(keyword.trim());
        }
        return discountRepository.findAll();
    }

    public DiscountBean getById(Integer id) {
        return discountRepository.findById(id).orElse(null);
    }

    public void save(DiscountBean discount) {
        // [新增容錯] 如果前端傳來 0，視為 null (新增模式)，避免 "找不到 ID=0" 的錯誤
        if (discount.getCouponId() != null && discount.getCouponId() == 0) {
            discount.setCouponId(null);
        }

        // [防呆] 如果有帶 ID (且不是0)，必須確保資料庫真的有這筆資料
        if (discount.getCouponId() != null) {
            if (!discountRepository.existsById(discount.getCouponId())) {
                throw new IllegalArgumentException("找不到 couponId=" + discount.getCouponId());
            }

            // 檢查是否沒有傳入新圖片，若無則保留舊圖
            if (discount.getCouponImg() == null || discount.getCouponImg().trim().isEmpty()) {
                discountRepository.findById(discount.getCouponId()).ifPresent(existing -> {
                    discount.setCouponImg(existing.getCouponImg());
                });
            }
        }
        discountRepository.save(discount);
    }

    public void delete(Integer id) {
        discountRepository.deleteById(id);
    }

    public boolean updateSingleStatus(Integer id, String action) {
        DiscountBean discount = getById(id);
        if (discount == null)
            return false;

        if ("disable".equalsIgnoreCase(action)) {
            discount.setCouponStatus(3);
        } else if ("relist".equalsIgnoreCase(action)) {
            LocalDate start = discount.getStartDate();
            LocalDate end = discount.getEndDate();

            if (start == null || end == null)
                return false;

            LocalDate today = LocalDate.now();
            if (today.isBefore(start))
                discount.setCouponStatus(0);
            else if (today.isAfter(end))
                discount.setCouponStatus(2);
            else
                discount.setCouponStatus(1);
        } else {
            return false;
        }

        discountRepository.save(discount);
        return true;
    }
}