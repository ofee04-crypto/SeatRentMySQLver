package com.example.backend.repository.merchantAndCoupon;

import com.example.backend.model.merchantAndCoupon.DiscountBean;
import org.springframework.data.jpa.repository.EntityGraph; // 必須有這個 import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<DiscountBean, Integer> {

        // [重點] 這裡一定要有 @EntityGraph，不然前端拿到資料會沒有 merchantName
        @Override
        @EntityGraph(attributePaths = "merchant")
        List<DiscountBean> findAll();

        @Override
        @EntityGraph(attributePaths = "merchant")
        Optional<DiscountBean> findById(Integer id);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query(value = "UPDATE d SET d.couponStatus = CASE " +
                        "WHEN m.merchantStatus = 0 THEN 3 " + // 商家停用優先權最高，強制下架
                        "WHEN d.endDate < CAST(GETDATE() AS DATE) THEN 2 " +
                        "WHEN d.startDate > CAST(GETDATE() AS DATE) THEN 0 " +
                        "ELSE 1 END " +
                        "FROM discount d " +
                        "JOIN merchant m ON d.merchantId = m.merchantId " + // 關聯商家表
                        "WHERE d.couponStatus != 3 OR m.merchantStatus = 0", nativeQuery = true)
        int autoUpdateStatus();

        // [重點] 這裡也要有
        @EntityGraph(attributePaths = "merchant")
        @Query("SELECT d FROM DiscountBean d LEFT JOIN d.merchant m " +
                        "WHERE d.couponName LIKE %:keyword% " +
                        "OR d.couponDescription LIKE %:keyword% " +
                        "OR m.merchantName LIKE %:keyword%") // 加上商家名稱搜尋，更符合直覺
        List<DiscountBean> findByKeyword(@Param("keyword") String keyword);

        // [重點] 這裡也要有
        @EntityGraph(attributePaths = "merchant")
        List<DiscountBean> findByMerchantId(Integer merchantId);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("UPDATE DiscountBean d SET d.couponStatus = 3 WHERE d.merchantId = :merchantId AND d.couponStatus != 3")
        void disableAllByMerchantId(@Param("merchantId") Integer merchantId);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query(value = "UPDATE discount SET couponStatus = CASE " +
                        "WHEN endDate < CAST(GETDATE() AS DATE) THEN 2 " +
                        "WHEN startDate > CAST(GETDATE() AS DATE) THEN 0 " +
                        "ELSE 1 END " +
                        "WHERE merchantId = :merchantId AND couponStatus = 3", nativeQuery = true)
        void relistAllByMerchantId(@Param("merchantId") Integer merchantId);
}