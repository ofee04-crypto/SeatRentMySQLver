package com.example.backend.controller.spot;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.model.spot.Seat;
import com.example.backend.service.spot.SeatService;

/**
 * [重構] 座位 (Seat) 資源的統一控制器
 * - 遵循 RESTful API 設計風格。
 * - 統一基礎路徑為 /api/seats
 */
@RestController
@RequestMapping("/seats") // [修正] 配合前端 Proxy 移除 /api 前綴的行為，改為監聽 /seats
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    // region 查詢功能 (Read)

    /**
     * 查詢所有座位 (GET /api/seats)
     */
    @GetMapping
    public List<Seat> getAllSeats() {
        return seatService.selectAll();
    }

    /**
     * 根據 ID 查詢單一座位 (GET /api/seats/{id})
     */
    @GetMapping("/{id}")
    public ResponseEntity<Seat> getSeatById(@PathVariable("id") Integer seatId) {
        Seat seat = seatService.selectById(seatId);
        return (seat != null) ? ResponseEntity.ok(seat) : ResponseEntity.notFound().build();
    }

    /**
     * 條件查詢 (GET /api/seats/search?seatsName=...)
     */
    @GetMapping("/search")
    public List<Seat> findSeatsByCondition(
            @RequestParam(required = false) String seatsName,
            @RequestParam(required = false) String seatsType,
            @RequestParam(required = false) String seatsStatus,
            @RequestParam(required = false) Integer spotId,
            @RequestParam(required = false) String serialNumber) {
        return seatService.findByCondition(seatsName, seatsType, seatsStatus, spotId, serialNumber);
    }

    // 根據 spotId 查詢座位數量 (GET /seats/count-by-spot)
    @GetMapping("/count-by-spot")
    public ResponseEntity<Long> getSeatCountBySpotId(@RequestParam("spotId") Integer spotId) {
        long count = seatService.countBySpotId(spotId);
        return ResponseEntity.ok(count);
    }

    // endregion

    // region 編輯功能 (Create / Update / Delete)

    /**
     * 新增座位 (POST /api/seats)
     */
    @PostMapping
    public ResponseEntity<Seat> createSeat(@RequestBody Seat seat) {
        Seat createdSeat = seatService.insert(seat);
        return new ResponseEntity<>(createdSeat, HttpStatus.CREATED);
    }

    /**
     * 更新座位 (PUT /api/seats/{id})
     * - 採用「先查詢、再修改、後儲存」的邏輯，確保資料完整性。
     */
    @PutMapping("/{id}")
    public ResponseEntity<Seat> updateSeat(@PathVariable("id") Integer seatId,
            @RequestBody Seat seatDetails) {
        // 1. 檢查 ID 是否一致 (防呆)
        if (seatDetails.getSeatsId() != null && !seatId.equals(seatDetails.getSeatsId())) {
            return ResponseEntity.badRequest().build();
        }

        // 2. 先從資料庫查出舊資料
        Seat existingSeat = seatService.selectById(seatId);
        if (existingSeat == null) {
            return ResponseEntity.notFound().build();
        }

        // 3. 手動更新欄位 (根據您的 Seat Entity 欄位進行調整)
        existingSeat.setSeatsName(seatDetails.getSeatsName());
        existingSeat.setSeatsType(seatDetails.getSeatsType());
        existingSeat.setSeatsStatus(seatDetails.getSeatsStatus());
        existingSeat.setSpotId(seatDetails.getSpotId());
        existingSeat.setSerialNumber(seatDetails.getSerialNumber());
        // existingSeat.setUpdatedAt(new Date()); // 如果 Service 沒有自動處理時間，這裡可能需要手動設

        // 4. 儲存更新
        Seat updatedSeat = seatService.update(existingSeat);
        return ResponseEntity.ok(updatedSeat);
    }

    /**
     * 刪除座位 (DELETE /api/seats/{id})
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSeat(@PathVariable("id") Integer seatId) {
        seatService.deleteById(seatId);
        return ResponseEntity.ok(Map.of("message", "Seat with ID " + seatId + " deleted successfully."));
    }

    // endregion

}