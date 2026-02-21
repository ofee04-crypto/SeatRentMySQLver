package com.example.backend.controller.maintenance;

import com.example.backend.model.maintenance.MaintenanceSchedule;
import com.example.backend.service.maintenance.MaintenanceScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 定期維護排程 Controller
 */
@RestController
@RequestMapping("/api/maintenance/schedule")
@CrossOrigin(origins = "http://localhost:5173")
public class MaintenanceScheduleController {

    private final MaintenanceScheduleService scheduleService;

    public MaintenanceScheduleController(MaintenanceScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // ==================== 查詢 ====================

    /**
     * 取得所有排程
     * GET /api/maintenance/schedule
     */
    @GetMapping
    public List<MaintenanceSchedule> getAllSchedules() {
        return scheduleService.getAllSchedules();
    }

    /**
     * 依 ID 取得單一排程
     * GET /api/maintenance/schedule/{id}
     */
    @GetMapping("/{id}")
    public MaintenanceSchedule getScheduleById(@PathVariable Integer id) {
        return scheduleService.getScheduleById(id);
    }

    /**
     * 取得所有啟用中的排程
     * GET /api/maintenance/schedule/active
     */
    @GetMapping("/active")
    public List<MaintenanceSchedule> getActiveSchedules() {
        return scheduleService.getActiveSchedules();
    }

    /**
     * 依目標查詢排程
     * GET /api/maintenance/schedule/target?type=SPOT&id=1
     */
    @GetMapping("/target")
    public List<MaintenanceSchedule> getSchedulesByTarget(
            @RequestParam("type") String targetType,
            @RequestParam("id") Integer targetId) {
        return scheduleService.getSchedulesByTarget(targetType.toUpperCase(), targetId);
    }

    /**
     * 取得已到期的排程 (供測試或手動觸發用)
     * GET /api/maintenance/schedule/due
     */
    @GetMapping("/due")
    public List<MaintenanceSchedule> getDueSchedules() {
        return scheduleService.getDueSchedules();
    }

    // ==================== 新增 ====================

    /**
     * 建立新排程
     * POST /api/maintenance/schedule
     */
    @PostMapping
    public ResponseEntity<MaintenanceSchedule> createSchedule(@RequestBody MaintenanceSchedule schedule) {
        MaintenanceSchedule created = scheduleService.createSchedule(schedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ==================== 更新 ====================

    /**
     * 更新排程
     * PUT /api/maintenance/schedule/{id}
     */
    @PutMapping("/{id}")
    public MaintenanceSchedule updateSchedule(
            @PathVariable Integer id,
            @RequestBody MaintenanceSchedule schedule) {
        schedule.setScheduleId(id);
        return scheduleService.updateSchedule(schedule);
    }

    /**
     * 切換啟用狀態
     * PATCH /api/maintenance/schedule/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public MaintenanceSchedule toggleActive(@PathVariable Integer id) {
        return scheduleService.toggleActive(id);
    }

    // ==================== 刪除 ====================

    /**
     * 刪除排程
     * DELETE /api/maintenance/schedule/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSchedule(@PathVariable Integer id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(Map.of("message", "排程已刪除"));
    }

    // ==================== 例外處理 ====================

    /**
     * 處理 IllegalArgumentException (驗證錯誤)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}
