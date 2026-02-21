package com.example.backend.controller.merchantAndCoupon;

import com.example.backend.dto.RedemptionLogDTO;
import com.example.backend.model.member.Member;
import com.example.backend.model.merchantAndCoupon.DiscountBean;
import com.example.backend.model.merchantAndCoupon.RedemptionLog;
import com.example.backend.model.merchantAndCoupon.Result;
import com.example.backend.repository.member.MemberRepository;
import com.example.backend.repository.merchantAndCoupon.RedemptionLogRepository;
import com.example.backend.service.merchantAndCoupon.DiscountService;
import com.example.backend.service.merchantAndCoupon.RedemptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.EmptyResultDataAccessException; // [新增]
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/discounts")
@CrossOrigin(origins = "http://localhost:5173") // 確保跨域配置正確
public class DiscountController {

    @Autowired
    private DiscountService discountService;
    @Autowired
    private MemberRepository memberRepository; // [新增] 注入會員 Repository 用於扣點

    @Autowired
    private RedemptionLogRepository redemptionLogRepository; // 注入新的 Repo

    @Autowired
    private RedemptionService redemptionService;

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    @GetMapping
    public Result<List<DiscountBean>> list(@RequestParam(required = false) String keyword) {
        return Result.success(discountService.getAll(keyword), "查詢成功");
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> save(
            @RequestPart("discount") String discountJson,
            @RequestPart(value = "image", required = false) MultipartFile file) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            DiscountBean discount = mapper.readValue(discountJson, DiscountBean.class);

            if (file != null && !file.isEmpty()) {
                // [安全性修正] 防止檔名為 null (NPE) 以及路徑穿越攻擊
                String originalFilename = file.getOriginalFilename();
                String safeFilename = (originalFilename == null || originalFilename.isBlank())
                        ? "upload.bin" // 預設檔名
                        : Paths.get(originalFilename).getFileName().toString();

                String fileName = UUID.randomUUID().toString() + "_" + safeFilename;

                Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
                if (!Files.exists(root))
                    Files.createDirectories(root);

                Files.copy(file.getInputStream(), root.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                discount.setCouponImg(fileName);
            }

            discountService.save(discount);
            return Result.success(null, "儲存成功");

        } catch (IllegalArgumentException e) {
            // [新增] 捕獲 Service 拋出的 "找不到 ID" 錯誤
            return Result.error("儲存失敗：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("伺服器錯誤: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        try {
            discountService.delete(id);
            return Result.success(null, "刪除成功");
        } catch (EmptyResultDataAccessException e) {
            // [防呆] 刪除不存在的 ID
            return Result.error("刪除失敗：找不到該優惠券");
        } catch (Exception e) {
            return Result.error("刪除失敗：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Integer id, @RequestParam String action) {
        boolean success = discountService.updateSingleStatus(id, action);
        if (success) {
            return Result.success(null, "狀態更新成功");
        } else {
            return Result.error("更新失敗：找不到資料、日期資料不全或指令錯誤");
        }
    }

    @PostMapping("/redeem")
    @Transactional // 確保 1.扣點 與 2.寫入紀錄 是一體的，要成功就一起成功
    public Result<Map<String, Object>> redeemCoupon(@RequestBody Map<String, Object> payload) {
        try {

            if (payload.get("memberId") == null || payload.get("couponId") == null || payload.get("passcode") == null) {
                return Result.error("核銷失敗：缺少必要參數 (memberId, couponId 或 passcode)");
            }
            Integer memberId = Integer.valueOf(payload.get("memberId").toString());
            Integer couponId = Integer.valueOf(payload.get("couponId").toString());
            String inputPasscode = payload.get("passcode").toString();

            // 1. 取得優惠券資訊
            DiscountBean coupon = discountService.getById(couponId);
            if (coupon == null)
                return Result.error("優惠券不存在");

            // 2. 驗證核銷碼
            if (!"1234".equals(inputPasscode))
                return Result.error("核銷失敗：核銷碼錯誤");

            // 3. 檢查會員點數
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("找不到會員"));

            if (member.getMemPoints() < coupon.getPointsRequired()) {
                return Result.error("點數不足，無法核銷");
            }

            // 4. 【核心動作 A】扣除會員點數
            int newPoints = member.getMemPoints() - coupon.getPointsRequired();
            member.setMemPoints(newPoints);
            memberRepository.save(member);

            // 5. 【核心動作 B】寫入兌換紀錄 (RedemptionLog)
            RedemptionLog log = new RedemptionLog();
            log.setMemId(memberId);
            log.setCouponId(couponId);
            log.setPointsSpent(coupon.getPointsRequired());
            log.setCouponName(coupon.getCouponName());
            // redeemTime 會由 SQL Server 的 DEFAULT GETDATE() 自動生成

            redemptionLogRepository.save(log);

            return Result.success(Map.of(
                    "currentPoints", newPoints,
                    "couponName", coupon.getCouponName()), "核銷成功！點數已扣除。");

        } catch (Exception e) {
            return Result.error("核銷失敗：" + e.getMessage());
        }
    }

    /**
     * 後台管理 API：取得所有兌換紀錄
     * 網址: GET /api/discounts/logs
     */
    @GetMapping("/logs")
    public Map<String, Object> getAllLogs() {
        // 抓取兌換紀錄
        List<RedemptionLog> list = redemptionLogRepository.findAllByOrderByRedeemTimeDesc();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("data", list);
        return result;
    }

    // 會員專屬紀錄 API
    @GetMapping("/member/{memId}/logs") // 建議加上 /member 區分路徑
    public ResponseEntity<List<RedemptionLogDTO>> getMemberLogs(@PathVariable Integer memId) {
        // 這裡會調用你之前寫的，帶有 MerchantName 的 Service 邏輯
        return ResponseEntity.ok(redemptionService.getMemberLogs(memId));
    }
}