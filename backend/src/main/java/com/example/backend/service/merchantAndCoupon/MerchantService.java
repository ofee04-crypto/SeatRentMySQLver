package com.example.backend.service.merchantAndCoupon;

import com.example.backend.model.merchantAndCoupon.MerchantBean;
import com.example.backend.repository.merchantAndCoupon.DiscountRepository;
import com.example.backend.repository.merchantAndCoupon.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MerchantService {

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private DiscountRepository discountRepository;

    public void deleteMerchant(int id) {
        merchantRepository.deleteById(id);
    }

    public MerchantBean getById(int id) {
        return merchantRepository.findById(id).orElse(null);
    }

    public List<MerchantBean> getAll() {
        return merchantRepository.findAll();
    }

    public List<MerchantBean> getByKeyword(String kw) {
        // [修正] 更嚴謹的判斷，如果 kw 是 null 或空字串，直接回傳全部，防止報錯
        if (kw == null || kw.trim().isEmpty()) {
            return merchantRepository.findAll();
        }
        return merchantRepository.findByKeyword(kw.trim());
    }

    public void saveOrUpdate(MerchantBean m) {
        // 1. 執行商家儲存
        merchantRepository.save(m);

        // 2. 核心邏輯：如果商家狀態被設為 0 (停用)
        if (m.getMerchantId() != null) {
            if (m.getMerchantStatus() == 0) {
                // 商家停用：強制所有優惠券下架 (狀態 3)
                discountRepository.disableAllByMerchantId(m.getMerchantId());
            } else if (m.getMerchantStatus() == 1) {
                // 商家啟用：觸發該商家優惠券的「狀態重算」
                // 這樣原本被強制下架(3)的優惠券，若還在有效日期內，就會變回 1
                discountRepository.relistAllByMerchantId(m.getMerchantId());
            }
        }
    }
}