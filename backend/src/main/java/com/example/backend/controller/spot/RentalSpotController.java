package com.example.backend.controller.spot;

import com.example.backend.dto.spot.SpotUpdateRequest;
import com.example.backend.model.spot.RentalSpot;
import com.example.backend.service.spot.RentalSpotService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * [重構] 租借據點 (RentalSpot) 資源的統一控制器
 * - 合併了 List, One, Insert, Update, Delete, ByCondition 等多個 Controller 的功能。
 * - 遵循 RESTful API 設計風格。
 */
@RestController
@RequestMapping("/spot") // [修正] 配合前端 Proxy 移除 /api 前綴的行為，改為監聽 /spot
public class RentalSpotController {

    @Value("${app.file.upload-path}")
    private String configUploadPath;

    private final RentalSpotService rentalSpotService;

    public RentalSpotController(RentalSpotService rentalSpotService) {
        this.rentalSpotService = rentalSpotService;
    }

    // region 查詢功能 (Read)

    /**
     * 查詢所有據點 (GET /api/spots)
     * (原 SpotListController)
     */
    @GetMapping("/list")
    public List<RentalSpot> getAllSpots() {
        return rentalSpotService.selectAll();
    }

    /**
     * [新增] 查詢所有據點及其即時座位數 (GET /api/spot/list-with-seats)
     * 專供首頁地圖與搜尋呈現使用，包含即時可用座位資訊。
     */
    @GetMapping("/list-with-seats")
    public List<com.example.backend.repository.projection.AnalyzeProjections.SpotWithSeats> getAllSpotsWithSeats() {
        return rentalSpotService.selectAllWithSeatCount();
    }

    /**
     * 根據 ID 查詢單一據點 (GET /api/spots/{id})
     * (原 SpotOneController)
     * 優化：使用 @PathVariable，讓 URL 更語意化。
     * 優化：使用 ResponseEntity 回傳，當找不到據點時回傳 404 Not Found。
     */
    @GetMapping("/{id}")
    public ResponseEntity<RentalSpot> getSpotById(@PathVariable("id") Integer spotId) {
        RentalSpot spot = rentalSpotService.selectById(spotId);
        return (spot != null) ? ResponseEntity.ok(spot) : ResponseEntity.notFound().build();
    }

    /**
     * 條件查詢 (GET /api/spots/search?spotName=...)
     * (原 SpotByConditionController)
     */
    @GetMapping("/search")
    public List<RentalSpot> findSpotsByCondition(@RequestParam(required = false) String spotCode,
            @RequestParam(required = false) String spotName, @RequestParam(required = false) String spotStatus,
            @RequestParam(required = false) Integer merchantId) {
        return rentalSpotService.findByCondition(spotCode, spotName, spotStatus, merchantId);
    }

    /**
     * [新增] 取得商家下拉選單資料 (GET /spot/merchants)
     * 供前端下拉選單使用，回傳商家 ID 與名稱
     */
    @GetMapping("/merchants")
    public ResponseEntity<List<Map<String, Object>>> getMerchantOptions() {
        return ResponseEntity.ok(rentalSpotService.findAllMerchantsForSelect());
    }

    // endregion

    // region 編輯功能 (Create / Update / Delete)

    /**
     * 新增據點 (POST /api/spots)
     * (原 SpotInsertController)
     * 優化：回傳 201 Created 狀態碼，表示資源已成功建立。
     */
    @PostMapping
    public ResponseEntity<RentalSpot> createSpot(@RequestBody RentalSpot spot) {
        RentalSpot createdSpot = rentalSpotService.insert(spot);
        return new ResponseEntity<>(createdSpot, HttpStatus.CREATED);
    }

    /**
     * 更新據點 (PUT /api/spots/{id})
     * (原 SpotUpdateController)
     * 優化：使用 PUT 方法表示「完整更新」一個已存在的資源。
     */
    @PutMapping("/{id}")
    public ResponseEntity<RentalSpot> updateSpot(@PathVariable("id") Integer spotId,
            @RequestBody SpotUpdateRequest updateRequest) {
        // 增加一個檢查，確保 URL 中的 ID 與請求內容的 ID 一致
        if (!spotId.equals(updateRequest.getSpotId())) {
            return ResponseEntity.badRequest().build(); // 回傳 400 錯誤請求
        }

        // 1. 先從資料庫查出舊資料
        RentalSpot spot = rentalSpotService.selectById(spotId);

        if (spot == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. 使用 BeanUtils 自動將 DTO 的值複製到 Entity
        // 這會自動比對欄位名稱，將 updateRequest 的值複製到 spot 中，省去手寫 set 的繁瑣
        BeanUtils.copyProperties(updateRequest, spot);

        // 3. 呼叫 Service 的 update 方法儲存
        RentalSpot updatedSpot = rentalSpotService.update(spot);
        return ResponseEntity.ok(updatedSpot);
    }

    /**
     * 刪除據點 (DELETE /api/spots/{id})
     * (原 SpotDeleteController)
     * 優化：使用 DELETE 方法，更符合 RESTful 風格。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSpot(@PathVariable("id") Integer spotId) {
        boolean isDeleted = rentalSpotService.deleteById(spotId);
        if (isDeleted) {
            return ResponseEntity.ok(Map.of("message", "Spot with ID " + spotId + " deleted successfully."));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 上傳據點圖片 (POST /api/spot/{id}/upload-image)
     * 將圖片儲存到檔案系統，資料庫只存檔案路徑
     */
    @PostMapping("/{id}/upload-image")
    public ResponseEntity<Map<String, String>> uploadSpotImage(
            @PathVariable("id") Integer spotId,
            @RequestParam("image") MultipartFile file) {

        try {
            // 1. 驗證檔案是否為空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "請選擇要上傳的圖片"));
            }

            // 2. 驗證檔案類型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "只能上傳圖片檔案 (jpg, png, gif)"));
            }

            // 3. 驗證檔案大小 (限制 5MB)
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "圖片大小不能超過 5MB"));
            }

            // 4. 查詢據點是否存在
            RentalSpot spot = rentalSpotService.selectById(spotId);
            if (spot == null) {
                return ResponseEntity.notFound().build();
            }

            // 5. 建立 uploads 目錄（如果不存在）
            String projectRoot = System.getProperty("user.dir");
            String uploadDir = projectRoot + "/" + configUploadPath + "/spots";
            java.io.File directory = new java.io.File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 6. 生成唯一檔案名稱（使用時間戳 + 原始副檔名）
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = "spot_" + spotId + "_" + System.currentTimeMillis() + fileExtension;
            String dbFilePath = configUploadPath + "/spots/" + fileName;

            // 7. 刪除舊圖片（如果存在）
            if (spot.getSpotImage() != null && !spot.getSpotImage().isEmpty()) {
                // 刪除時也要用絕對路徑找檔案
                java.io.File oldFile = new java.io.File(projectRoot, spot.getSpotImage());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
            }

            // 8. 儲存檔案到檔案系統
            java.io.File destFile = new java.io.File(directory, fileName);
            file.transferTo(destFile);

            // 9. 更新據點的圖片欄位（儲存相對路徑）
            spot.setSpotImage(dbFilePath);
            rentalSpotService.update(spot);

            // 10. 回傳檔案 URL（前端可用於顯示）
            String imageUrl = "/" + dbFilePath; // 前端透過靜態資源存取

            return ResponseEntity.ok(Map.of(
                    "message", "圖片上傳成功",
                    "imageUrl", imageUrl,
                    "filePath", dbFilePath));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "圖片上傳失敗: " + e.getMessage()));
        }
    }

    /**
     * 刪除據點圖片 (DELETE /api/spot/{id}/image)
     * 刪除檔案系統中的圖片檔案，並清空資料庫欄位
     */
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Map<String, String>> deleteSpotImage(@PathVariable("id") Integer spotId) {
        RentalSpot spot = rentalSpotService.selectById(spotId);
        if (spot == null) {
            return ResponseEntity.notFound().build();
        }

        // 刪除檔案系統中的圖片
        if (spot.getSpotImage() != null && !spot.getSpotImage().isEmpty()) {
            String projectRoot = System.getProperty("user.dir");
            java.io.File file = new java.io.File(projectRoot, spot.getSpotImage());
            if (file.exists()) {
                file.delete();
            }
        }

        // 清空資料庫欄位
        spot.setSpotImage(null);
        rentalSpotService.update(spot);

        return ResponseEntity.ok(Map.of("message", "圖片已刪除"));
    }

    // endregion
}
