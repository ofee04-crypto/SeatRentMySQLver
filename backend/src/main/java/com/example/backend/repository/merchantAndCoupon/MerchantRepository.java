package com.example.backend.repository.merchantAndCoupon;

import com.example.backend.model.merchantAndCoupon.MerchantBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // 記得 import
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantRepository extends JpaRepository<MerchantBean, Integer> {

    // [編譯安全] 加上 @Param
    @Query("SELECT m FROM MerchantBean m WHERE m.merchantName LIKE %:keyword% OR m.merchantAddress LIKE %:keyword%")
    List<MerchantBean> findByKeyword(@Param("keyword") String keyword);
}