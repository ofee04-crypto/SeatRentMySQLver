package com.example.backend.controller.merchantAndCoupon;

import com.example.backend.model.merchantAndCoupon.MerchantBean;
import com.example.backend.model.merchantAndCoupon.Result; 
import com.example.backend.service.merchantAndCoupon.MerchantService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException; // [新增]
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/merchants")

public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    @GetMapping
    public Result<List<MerchantBean>> list(@RequestParam(required = false) String keyword) {
        log.info("開始查詢商家列表, 關鍵字: {}", keyword);
        try {
            List<MerchantBean> list;
            if (keyword != null && !keyword.trim().isEmpty()) {
                list = merchantService.getByKeyword(keyword);
            } else {
                list = merchantService.getAll();
            }
            log.info("查詢成功，共 {} 筆資料", list.size());
            return Result.success(list, "查詢成功");
        } catch (Exception e) {
            log.error("查詢商家失敗", e);
            return Result.error("伺服器內部錯誤：" + e.getMessage());
        }
    }

    @PostMapping
    public Result<String> create(@RequestBody MerchantBean merchant) {
        log.info("收到新增商家請求: {}", merchant.getMerchantName());
        try {
            merchant.setMerchantId(null);
            merchantService.saveOrUpdate(merchant);
            return Result.success(null, "新增商家成功");
        } catch (Exception e) {
            log.error("新增商家失敗", e);
            return Result.error("新增失敗: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody MerchantBean merchant) {
        log.info("收到更新商家請求, ID: {}, 資料: {}", id, merchant);
        try {
            MerchantBean existing = merchantService.getById(id);
            if (existing == null) {
                log.warn("更新失敗，找不到 ID 為 {} 的商家", id);
                return Result.error("更新失敗：找不到該商家資料");
            }
            merchant.setMerchantId(id);
            merchantService.saveOrUpdate(merchant);

            log.info("商家 ID: {} 更新成功", id);
            return Result.success(null, "資料更新成功");
        } catch (Exception e) {
            log.error("更新商家過程發生異常", e);
            return Result.error("更新過程中發生錯誤: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        log.warn("收到刪除商家請求, ID: {}", id);
        try {
            merchantService.deleteMerchant(id);
            log.info("商家 ID: {} 已成功刪除", id);
            return Result.success(null, "刪除成功");
        } catch (EmptyResultDataAccessException e) {
            // [新增] 找不到 ID 時的處理
            return Result.error("刪除失敗：找不到該商家");
        } catch (Exception e) {
            log.error("刪除商家失敗", e);
            String msg = String.valueOf(e.getMessage());
            if (msg.contains("ConstraintViolationException") || msg.toLowerCase().contains("foreign key")) {
                return Result.error("刪除失敗：該商家旗下還有關聯資料（如優惠券），請先刪除關聯資料。");
            }
            return Result.error("刪除失敗: " + msg);
        }
    }
}