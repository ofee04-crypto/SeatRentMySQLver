package com.example.backend.service.maintenance;

import com.example.backend.model.maintenance.MaintenanceInformation;
import com.example.backend.model.maintenance.MaintenanceSchedule;
import com.example.backend.model.spot.Seat;
import com.example.backend.repository.maintenance.MaintenanceInformationRepository;
import com.example.backend.repository.maintenance.MaintenanceScheduleRepository;
import com.example.backend.repository.spot.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 定期維護排程執行器
 * 每分鐘檢查到期的排程，並自動建立工單
 */
@Component
public class MaintenanceJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceJobScheduler.class);
    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");

    /**
     * 未結案狀態清單 (用於防爆單檢查)
     */
    private static final List<String> OPEN_STATUSES = Arrays.asList(
            "REPORTED",          // 已回報
            "ASSIGNED",          // 已指派
            "UNDER_MAINTENANCE"  // 維修中
    );

    private final MaintenanceScheduleRepository scheduleRepo;
    private final MaintenanceInformationRepository ticketRepo;
    private final SeatRepository seatRepo;

    public MaintenanceJobScheduler(
            MaintenanceScheduleRepository scheduleRepo,
            MaintenanceInformationRepository ticketRepo,
            SeatRepository seatRepo) {
        this.scheduleRepo = scheduleRepo;
        this.ticketRepo = ticketRepo;
        this.seatRepo = seatRepo;
    }

    /**
     * 每分鐘執行一次，檢查到期的排程
     */
    @Scheduled(cron = "0 * * * * *") // 每分鐘的第 0 秒執行
    @Transactional
    public void executeScheduledMaintenance() {
        LocalDateTime now = ZonedDateTime.now(TAIPEI_ZONE).toLocalDateTime();
        log.info("⏰ [排程檢查] 開始檢查到期排程，當前時間: {}", now);

        // 1. 查詢所有到期且啟用的排程
        List<MaintenanceSchedule> dueSchedules = scheduleRepo.findByNextExecuteAtBeforeAndIsActiveTrue(now);

        if (dueSchedules.isEmpty()) {
            log.debug("📭 [排程檢查] 沒有到期的排程");
            return;
        }

        log.info("📋 [排程檢查] 找到 {} 筆到期排程", dueSchedules.size());

        // 2. 逐一處理每個排程
        for (MaintenanceSchedule schedule : dueSchedules) {
            try {
                processSchedule(schedule, now);
            } catch (Exception e) {
                log.error("❌ [排程執行] 排程 ID={} 執行失敗: {}", schedule.getScheduleId(), e.getMessage(), e);
            }
        }

        log.info("✅ [排程檢查] 本次檢查完成");
    }

    /**
     * 處理單一排程
     */
    private void processSchedule(MaintenanceSchedule schedule, LocalDateTime now) {
        Integer scheduleId = schedule.getScheduleId();
        String targetType = schedule.getTargetType();
        Integer targetId = schedule.getTargetId();

        log.info("🔧 [排程執行] 處理排程 ID={}, 標題={}, 目標={}:{}",
                scheduleId, schedule.getTitle(), targetType, targetId);

        // 1. 防爆單檢查
        boolean hasOpenTicket = checkHasOpenTicket(targetType, targetId);

        if (hasOpenTicket) {
            // 有未結案工單，跳過開單，只更新時間
            log.warn("⚠️ [防爆單] 目標 {}:{} 已有未結案工單，跳過開單", targetType, targetId);
            updateScheduleTime(schedule, now);
            return;
        }

        // 2. 建立新工單
        MaintenanceInformation ticket = createTicket(schedule);
        ticketRepo.save(ticket);
        log.info("✅ [開單成功] 工單已建立，排程 ID={}, 工單 ID={}", scheduleId, ticket.getTicketId());

        // 3. 更新排程時間
        updateScheduleTime(schedule, now);
    }

    /**
     * 檢查是否有未結案工單 (防爆單)
     */
    private boolean checkHasOpenTicket(String targetType, Integer targetId) {
        if ("SPOT".equals(targetType)) {
            // SPOT: 檢查該機台是否有未結案工單 (且 seatsId 為 null，排除椅子的工單)
            return ticketRepo.existsBySpotIdAndSeatsIdIsNullAndIssueStatusIn(targetId, OPEN_STATUSES);
        } else if ("SEAT".equals(targetType)) {
            // SEAT: 檢查該椅子是否有未結案工單
            return ticketRepo.existsBySeatsIdAndIssueStatusIn(targetId, OPEN_STATUSES);
        }
        return false;
    }

    /**
     * 建立工單
     */
    private MaintenanceInformation createTicket(MaintenanceSchedule schedule) {
        MaintenanceInformation ticket = new MaintenanceInformation();

        // 基本欄位
        ticket.setIssueType(schedule.getIssueType());
        ticket.setIssuePriority(schedule.getIssuePriority());
        ticket.setIssueStatus("REPORTED"); // 初始狀態：已回報

        // 指派人員 (如果有設定)
        if (schedule.getAssignedStaffId() != null) {
            ticket.setAssignedStaffId(schedule.getAssignedStaffId());
            ticket.setIssueStatus("ASSIGNED"); // 有指派就直接設為已指派
        }

        // 根據 targetType 設定 spotId / seatsId
        String targetType = schedule.getTargetType();
        Integer targetId = schedule.getTargetId();

        if ("SPOT".equals(targetType)) {
            // 機台排程：直接設定 spotId
            ticket.setSpotId(targetId);
            ticket.setIssueDesc(String.format("【排程自動保養】%s", schedule.getTitle()));

        } else if ("SEAT".equals(targetType)) {
            // 椅子排程：需要查詢椅子所屬的 spotId
            Seat seat = seatRepo.findById(targetId)
                    .orElseThrow(() -> new IllegalStateException("找不到椅子 ID: " + targetId));

            ticket.setSpotId(seat.getSpotId());   // 設定椅子所屬的機台
            ticket.setSeatsId(targetId);          // 設定椅子 ID

            // 備註加上椅子資訊
            String seatInfo = seat.getSeatsName() != null ? seat.getSeatsName() : String.valueOf(targetId);
            ticket.setIssueDesc(String.format("【排程自動保養】%s - 椅子編號: %s", schedule.getTitle(), seatInfo));
        }

        return ticket;
    }

    /**
     * 更新排程時間 (lastExecutedAt + nextExecuteAt)
     */
    private void updateScheduleTime(MaintenanceSchedule schedule, LocalDateTime now) {
        // 更新上次執行時間
        schedule.setLastExecutedAt(now);

        // 計算下次執行時間
        LocalDateTime nextExecuteAt = calculateNextExecuteTime(
                schedule.getScheduleType(),
                schedule.getExecuteTime(),
                schedule.getDayOfWeek(),
                schedule.getDayOfMonth()
        );
        schedule.setNextExecuteAt(nextExecuteAt);

        scheduleRepo.save(schedule);
        log.info("📅 [時間更新] 排程 ID={}, 下次執行時間={}", schedule.getScheduleId(), nextExecuteAt);
    }

    // ==================== 計算下次執行時間 ====================

    /**
     * 計算下次執行時間
     */
    private LocalDateTime calculateNextExecuteTime(
            String scheduleType,
            LocalTime executeTime,
            Integer dayOfWeek,
            Integer dayOfMonth) {

        ZonedDateTime now = ZonedDateTime.now(TAIPEI_ZONE);

        switch (scheduleType) {
            case "DAILY":
                // 每日：明天同一時間
                return now.toLocalDate().plusDays(1).atTime(executeTime);

            case "WEEKLY":
                // 每週：下週同一天
                return now.toLocalDate().plusWeeks(1)
                        .with(java.time.DayOfWeek.of(dayOfWeek))
                        .atTime(executeTime);

            case "MONTHLY":
                // 每月：下個月同一天
                return getNextMonthDate(now, executeTime, dayOfMonth);

            default:
                throw new IllegalArgumentException("未知的排程類型: " + scheduleType);
        }
    }

    /**
     * 取得下個月對應日期的 LocalDateTime
     */
    private LocalDateTime getNextMonthDate(ZonedDateTime now, LocalTime executeTime, Integer dayOfMonth) {
        ZonedDateTime nextMonth = now.plusMonths(1);
        int maxDayOfNextMonth = nextMonth.toLocalDate().withDayOfMonth(1).lengthOfMonth();
        int actualDay = Math.min(dayOfMonth, maxDayOfNextMonth);

        return LocalDateTime.of(
                nextMonth.getYear(),
                nextMonth.getMonthValue(),
                actualDay,
                executeTime.getHour(),
                executeTime.getMinute(),
                executeTime.getSecond()
        );
    }
}
