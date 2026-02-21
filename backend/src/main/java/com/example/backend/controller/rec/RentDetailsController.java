package com.example.backend.controller.rec;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.rec.DailyOrderCountDTO;
import com.example.backend.dto.rec.HourlyOrderCountDTO;
import com.example.backend.dto.rec.MonthlyOrderCountDTO;
import com.example.backend.dto.rec.OrderStatusStatsDTO;
import com.example.backend.dto.rec.RentalDurationDTO;
import com.example.backend.dto.rec.RentalDurationStatsDTO;
import com.example.backend.model.rec.RentDetails;
import com.example.backend.repository.rec.RentDetailsRepository;
import com.example.backend.service.rec.RecDetailMgnService;

@RestController
@RequestMapping("/api/rent-details")
@CrossOrigin // 允許前端跨域呼叫
public class RentDetailsController {

    @Autowired
    private RentDetailsRepository rentDetailsRepository;

    @Autowired
    private RecDetailMgnService recDetailMgnService;

    // 1. 搜尋全部
    @GetMapping("/all")
    public List<RentDetails> getAll() {
        return rentDetailsRepository.findAll();
    }

    // 2. 依 recID 搜尋
    @GetMapping("/{id}")
    public RentDetails getById(@PathVariable String id) {
        return rentDetailsRepository.findById(id).orElse(null);
    }

    // 3. 依時間區間取得每月訂單統計數據
    @GetMapping("/stats/monthly-orders")
    public ResponseEntity<List<MonthlyOrderCountDTO>> getMonthlyOrderStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<MonthlyOrderCountDTO> stats = recDetailMgnService.getMonthlyOrderCounts(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // 4. 依時間區間取得訂單狀態統計（圓餅圖）
    @GetMapping("/stats/order-status")
    public ResponseEntity<List<OrderStatusStatsDTO>> getOrderStatusStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<OrderStatusStatsDTO> stats = recDetailMgnService.getOrderStatusStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // 5. 依時間區間取得訂單租借時長（散佈圖）
    @GetMapping("/stats/rental-duration")
    public ResponseEntity<List<RentalDurationDTO>> getRentalDurations(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<RentalDurationDTO> stats = recDetailMgnService.getRentalDurations(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // 6. 依時間區間取得每小時訂單統計
    @GetMapping("/stats/hourly-orders")
    public ResponseEntity<List<HourlyOrderCountDTO>> getHourlyOrderStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<HourlyOrderCountDTO> stats = recDetailMgnService.getHourlyOrderCounts(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // 7. 依時間區間取得每日訂單統計
    @GetMapping("/stats/daily-orders")
    public ResponseEntity<List<DailyOrderCountDTO>> getDailyOrderStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DailyOrderCountDTO> stats = recDetailMgnService.getDailyOrderCounts(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // 8. 依時間區間取得租借時長統計 (30分鐘間隔)
    @GetMapping("/stats/rental-duration-intervals")
    public ResponseEntity<List<RentalDurationStatsDTO>> getRentalDurationStats(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<RentalDurationStatsDTO> stats = recDetailMgnService.getRentalDurationStats(startDate, endDate);
        return ResponseEntity.ok(stats);
    }
}
