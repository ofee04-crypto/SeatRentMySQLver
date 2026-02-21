package com.example.backend.service.maintenance;

import com.example.backend.dto.maintenance.MaintenanceScheduleResponseDto;
import com.example.backend.model.maintenance.MaintenanceSchedule;
import com.example.backend.model.maintenance.MaintenanceStaff;
import com.example.backend.repository.maintenance.MaintenanceScheduleRepository;
import com.example.backend.repository.maintenance.MaintenanceStaffRepository;
import com.example.backend.repository.spot.RentalSpotRepository;
import com.example.backend.repository.spot.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 定期維護排程 Service
 */
@Service
@Transactional
public class MaintenanceScheduleService {

    private static final ZoneId TAIPEI_ZONE = ZoneId.of("Asia/Taipei");

    private final MaintenanceScheduleRepository scheduleRepo;
    private final MaintenanceStaffRepository staffRepo;
    private final RentalSpotRepository spotRepo;
    private final SeatRepository seatRepo;

    public MaintenanceScheduleService(
            MaintenanceScheduleRepository scheduleRepo,
            MaintenanceStaffRepository staffRepo,
            RentalSpotRepository spotRepo,
            SeatRepository seatRepo) {
        this.scheduleRepo = scheduleRepo;
        this.staffRepo = staffRepo;
        this.spotRepo = spotRepo;
        this.seatRepo = seatRepo;
    }

    // ==================== 查詢 ====================

    /**
     * 取得所有排程 (含人員姓名 DTO)
     * 注意：Controller 可能聲明返回 List<MaintenanceSchedule>，但 DTO 會被正確序列化
     */
    @SuppressWarnings("unchecked")
    public List getAllSchedules() {
        List<MaintenanceSchedule> schedules = scheduleRepo.findAll();
        return mapToDtoList(schedules);
    }

    /**
     * 依 ID 取得排程 (Entity)
     */
    public MaintenanceSchedule getScheduleById(Integer id) {
        return scheduleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到排程 ID: " + id));
    }

    /**
     * 取得所有啟用中的排程 (含人員姓名 DTO)
     */
    @SuppressWarnings("unchecked")
    public List getActiveSchedules() {
        List<MaintenanceSchedule> schedules = scheduleRepo.findByIsActive(true);
        return mapToDtoList(schedules);
    }

    /**
     * 取得特定目標的排程 (含人員姓名 DTO)
     */
    @SuppressWarnings("unchecked")
    public List getSchedulesByTarget(String targetType, Integer targetId) {
        List<MaintenanceSchedule> schedules = scheduleRepo.findByTargetTypeAndTargetId(targetType, targetId);
        return mapToDtoList(schedules);
    }

    /**
     * 查詢已到期且啟用的排程 (Entity - 供定時任務使用)
     */
    public List<MaintenanceSchedule> getDueSchedulesEntity() {
        LocalDateTime now = ZonedDateTime.now(TAIPEI_ZONE).toLocalDateTime();
        return scheduleRepo.findByNextExecuteAtBeforeAndIsActiveTrue(now);
    }

    /**
     * 查詢已到期且啟用的排程 (含人員姓名 DTO)
     */
    @SuppressWarnings("unchecked")
    public List getDueSchedules() {
        LocalDateTime now = ZonedDateTime.now(TAIPEI_ZONE).toLocalDateTime();
        List<MaintenanceSchedule> schedules = scheduleRepo.findByNextExecuteAtBeforeAndIsActiveTrue(now);
        return mapToDtoList(schedules);
    }

    // ==================== 新增 ====================

    /**
     * 建立新排程
     */
    public MaintenanceSchedule createSchedule(MaintenanceSchedule schedule) {
        // 1. 驗證資料
        validateSchedule(schedule);

        // 2. 計算下次執行時間
        LocalDateTime nextExecuteAt = calculateNextExecuteTime(
                schedule.getScheduleType(),
                schedule.getExecuteTime(),
                schedule.getDayOfWeek(),
                schedule.getDayOfMonth()
        );
        schedule.setNextExecuteAt(nextExecuteAt);

        // 3. 設定預設值
        if (schedule.getIsActive() == null) {
            schedule.setIsActive(true);
        }
        if (schedule.getIssuePriority() == null) {
            schedule.setIssuePriority("NORMAL");
        }

        return scheduleRepo.save(schedule);
    }

    // ==================== 更新 ====================

    /**
     * 更新排程
     */
    public MaintenanceSchedule updateSchedule(MaintenanceSchedule schedule) {
        // 1. 確認排程存在
        MaintenanceSchedule existing = getScheduleById(schedule.getScheduleId());

        // 2. 驗證資料
        validateSchedule(schedule);

        // 3. 更新欄位
        existing.setTitle(schedule.getTitle());
        existing.setTargetType(schedule.getTargetType());
        existing.setTargetId(schedule.getTargetId());
        existing.setScheduleType(schedule.getScheduleType());
        existing.setDayOfWeek(schedule.getDayOfWeek());
        existing.setDayOfMonth(schedule.getDayOfMonth());
        existing.setExecuteTime(schedule.getExecuteTime());
        existing.setIssueType(schedule.getIssueType());
        existing.setIssuePriority(schedule.getIssuePriority());
        existing.setAssignedStaffId(schedule.getAssignedStaffId());
        existing.setIsActive(schedule.getIsActive());

        // 4. 重新計算下次執行時間
        LocalDateTime nextExecuteAt = calculateNextExecuteTime(
                schedule.getScheduleType(),
                schedule.getExecuteTime(),
                schedule.getDayOfWeek(),
                schedule.getDayOfMonth()
        );
        existing.setNextExecuteAt(nextExecuteAt);

        return scheduleRepo.save(existing);
    }

    /**
     * 切換啟用狀態
     */
    public MaintenanceSchedule toggleActive(Integer id) {
        MaintenanceSchedule schedule = getScheduleById(id);
        schedule.setIsActive(!schedule.getIsActive());

        // 如果重新啟用，重新計算下次執行時間
        if (schedule.getIsActive()) {
            LocalDateTime nextExecuteAt = calculateNextExecuteTime(
                    schedule.getScheduleType(),
                    schedule.getExecuteTime(),
                    schedule.getDayOfWeek(),
                    schedule.getDayOfMonth()
            );
            schedule.setNextExecuteAt(nextExecuteAt);
        }

        return scheduleRepo.save(schedule);
    }

    // ==================== 刪除 ====================

   
  

    // (軟刪除) 刪除排程
     
    public void deleteSchedule(Integer id) {
        // 1. 先把排程抓出來
        MaintenanceSchedule schedule = getScheduleById(id);
        
        // 2. 標記為停用 (isActive = false)
        schedule.setIsActive(false);
        
        // 3. 儲存
        scheduleRepo.save(schedule);
}

    // ==================== 執行後更新 ====================

    /**
     * 排程執行後，更新執行時間
     * (供定時任務在產生工單後呼叫)
     */
    public void markExecuted(Integer scheduleId) {
        MaintenanceSchedule schedule = getScheduleById(scheduleId);
        LocalDateTime now = ZonedDateTime.now(TAIPEI_ZONE).toLocalDateTime();

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
    }

    // ==================== 私有方法：驗證 ====================

    /**
     * 驗證排程資料
     */
    private void validateSchedule(MaintenanceSchedule schedule) {
        // 基本欄位檢查
        if (isBlank(schedule.getTitle())) {
            throw new IllegalArgumentException("標題不能為空");
        }
        if (isBlank(schedule.getTargetType())) {
            throw new IllegalArgumentException("目標類型不能為空");
        }
        if (schedule.getTargetId() == null) {
            throw new IllegalArgumentException("目標 ID 不能為空");
        }
        if (isBlank(schedule.getScheduleType())) {
            throw new IllegalArgumentException("排程類型不能為空");
        }
        if (schedule.getExecuteTime() == null) {
            throw new IllegalArgumentException("執行時間不能為空");
        }
        if (isBlank(schedule.getIssueType())) {
            throw new IllegalArgumentException("問題類型不能為空");
        }

        // 驗證 targetType
        String targetType = schedule.getTargetType().toUpperCase();
        if (!targetType.equals("SPOT") && !targetType.equals("SEAT")) {
            throw new IllegalArgumentException("目標類型必須是 SPOT 或 SEAT");
        }
        schedule.setTargetType(targetType);

        // 驗證 targetId 是否存在
        validateTargetExists(targetType, schedule.getTargetId());

        // 驗證 scheduleType 和對應欄位
        String scheduleType = schedule.getScheduleType().toUpperCase();
        schedule.setScheduleType(scheduleType);

        switch (scheduleType) {
            case "DAILY":
                // DAILY: dayOfWeek/dayOfMonth 必須為 null
                if (schedule.getDayOfWeek() != null || schedule.getDayOfMonth() != null) {
                    throw new IllegalArgumentException("每日排程不需要指定 dayOfWeek 或 dayOfMonth");
                }
                break;

            case "WEEKLY":
                // WEEKLY: dayOfWeek 必須 1-7
                if (schedule.getDayOfWeek() == null) {
                    throw new IllegalArgumentException("每週排程必須指定 dayOfWeek (1-7)");
                }
                if (schedule.getDayOfWeek() < 1 || schedule.getDayOfWeek() > 7) {
                    throw new IllegalArgumentException("dayOfWeek 必須介於 1-7 (週一至週日)");
                }
                if (schedule.getDayOfMonth() != null) {
                    throw new IllegalArgumentException("每週排程不需要指定 dayOfMonth");
                }
                break;

            case "MONTHLY":
                // MONTHLY: dayOfMonth 必須 1-31
                if (schedule.getDayOfMonth() == null) {
                    throw new IllegalArgumentException("每月排程必須指定 dayOfMonth (1-31)");
                }
                if (schedule.getDayOfMonth() < 1 || schedule.getDayOfMonth() > 31) {
                    throw new IllegalArgumentException("dayOfMonth 必須介於 1-31");
                }
                if (schedule.getDayOfWeek() != null) {
                    throw new IllegalArgumentException("每月排程不需要指定 dayOfWeek");
                }
                break;

            default:
                throw new IllegalArgumentException("排程類型必須是 DAILY, WEEKLY 或 MONTHLY");
        }
    }

    /**
     * 驗證目標是否存在
     */
    private void validateTargetExists(String targetType, Integer targetId) {
        boolean exists;
        if ("SPOT".equals(targetType)) {
            exists = spotRepo.existsById(targetId);
            if (!exists) {
                throw new IllegalArgumentException("找不到機台 ID: " + targetId);
            }
        } else if ("SEAT".equals(targetType)) {
            exists = seatRepo.existsById(targetId);
            if (!exists) {
                throw new IllegalArgumentException("找不到座位 ID: " + targetId);
            }
        }
    }

    // ==================== 私有方法：計算下次執行時間 ====================

    /**
     * 計算下次執行時間
     *
     * @param scheduleType 排程類型 (DAILY/WEEKLY/MONTHLY)
     * @param executeTime  執行時間
     * @param dayOfWeek    星期幾 (1=週一, 7=週日)
     * @param dayOfMonth   每月幾號
     * @return 下次執行的 LocalDateTime
     */
    private LocalDateTime calculateNextExecuteTime(
            String scheduleType,
            LocalTime executeTime,
            Integer dayOfWeek,
            Integer dayOfMonth) {

        ZonedDateTime now = ZonedDateTime.now(TAIPEI_ZONE);
        LocalDateTime todayAtTime = now.toLocalDate().atTime(executeTime);

        switch (scheduleType) {
            case "DAILY":
                return calculateNextDaily(now, todayAtTime);

            case "WEEKLY":
                return calculateNextWeekly(now, executeTime, dayOfWeek);

            case "MONTHLY":
                return calculateNextMonthly(now, executeTime, dayOfMonth);

            default:
                throw new IllegalArgumentException("未知的排程類型: " + scheduleType);
        }
    }

    /**
     * 計算每日排程的下次執行時間
     */
    private LocalDateTime calculateNextDaily(ZonedDateTime now, LocalDateTime todayAtTime) {
        // 如果今天的執行時間還沒過，就是今天；否則是明天
        if (now.toLocalDateTime().isBefore(todayAtTime)) {
            return todayAtTime;
        } else {
            return todayAtTime.plusDays(1);
        }
    }

    /**
     * 計算每週排程的下次執行時間
     * dayOfWeek: 1=週一, 2=週二, ..., 7=週日
     */
    private LocalDateTime calculateNextWeekly(ZonedDateTime now, LocalTime executeTime, Integer dayOfWeek) {
        // Java DayOfWeek: MONDAY=1, SUNDAY=7 (與我們的定義一致)
        int currentDayOfWeek = now.getDayOfWeek().getValue();
        int daysUntilTarget = dayOfWeek - currentDayOfWeek;

        LocalDateTime targetDateTime;

        if (daysUntilTarget > 0) {
            // 目標日在本週後面
            targetDateTime = now.toLocalDate().plusDays(daysUntilTarget).atTime(executeTime);
        } else if (daysUntilTarget < 0) {
            // 目標日已過，下週
            targetDateTime = now.toLocalDate().plusDays(daysUntilTarget + 7).atTime(executeTime);
        } else {
            // 就是今天
            targetDateTime = now.toLocalDate().atTime(executeTime);
            // 如果時間已過，等下週
            if (now.toLocalDateTime().isAfter(targetDateTime) || now.toLocalDateTime().isEqual(targetDateTime)) {
                targetDateTime = targetDateTime.plusDays(7);
            }
        }

        return targetDateTime;
    }

    /**
     * 計算每月排程的下次執行時間
     */
    private LocalDateTime calculateNextMonthly(ZonedDateTime now, LocalTime executeTime, Integer dayOfMonth) {
        int currentDay = now.getDayOfMonth();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        LocalDateTime targetDateTime;

        // 處理月份天數不足的情況 (例如 2 月沒有 30, 31 號)
        int maxDayOfCurrentMonth = now.toLocalDate().lengthOfMonth();
        int actualDay = Math.min(dayOfMonth, maxDayOfCurrentMonth);

        if (currentDay < actualDay) {
            // 本月的目標日還沒到
            targetDateTime = LocalDateTime.of(currentYear, currentMonth, actualDay, 
                    executeTime.getHour(), executeTime.getMinute(), executeTime.getSecond());
        } else if (currentDay == actualDay) {
            // 就是今天
            targetDateTime = LocalDateTime.of(currentYear, currentMonth, actualDay,
                    executeTime.getHour(), executeTime.getMinute(), executeTime.getSecond());
            // 如果時間已過，等下個月
            if (now.toLocalDateTime().isAfter(targetDateTime) || now.toLocalDateTime().isEqual(targetDateTime)) {
                targetDateTime = getNextMonthDate(now, executeTime, dayOfMonth);
            }
        } else {
            // 本月的目標日已過，等下個月
            targetDateTime = getNextMonthDate(now, executeTime, dayOfMonth);
        }

        return targetDateTime;
    }

    /**
     * 取得下個月對應日期的 LocalDateTime
     */
    private LocalDateTime getNextMonthDate(ZonedDateTime now, LocalTime executeTime, Integer dayOfMonth) {
        // 移到下個月
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

    // ==================== DTO 轉換方法 ====================

    /**
     * 批量轉換 Entity 為 DTO (避免 N+1 查詢)
     */
    private List<MaintenanceScheduleResponseDto> mapToDtoList(List<MaintenanceSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return List.of();
        }

        // 收集所有 staffId (去重、排除 null)
        List<Integer> staffIds = schedules.stream()
                .map(MaintenanceSchedule::getAssignedStaffId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 一次查詢所有人員，建立 Map<id, name>
        Map<Integer, String> staffNameMap = staffIds.isEmpty() 
                ? Map.of()
                : staffRepo.findAllById(staffIds).stream()
                        .collect(Collectors.toMap(
                                MaintenanceStaff::getStaffId,
                                MaintenanceStaff::getStaffName
                        ));

        // 轉換為 DTO
        return schedules.stream()
                .map(entity -> mapToDto(entity, staffNameMap))
                .collect(Collectors.toList());
    }

    /**
     * 單筆轉換 Entity 為 DTO
     */
    private MaintenanceScheduleResponseDto mapToDto(MaintenanceSchedule entity, Map<Integer, String> staffNameMap) {
        String staffName = "未指定";
        if (entity.getAssignedStaffId() != null) {
            staffName = staffNameMap.getOrDefault(entity.getAssignedStaffId(), "未指定");
        }

        return new MaintenanceScheduleResponseDto(
                entity.getScheduleId(),
                entity.getTitle(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getScheduleType(),
                entity.getDayOfWeek(),
                entity.getDayOfMonth(),
                entity.getExecuteTime(),
                entity.getIssueType(),
                entity.getIssuePriority(),
                entity.getAssignedStaffId(),
                staffName,
                entity.getIsActive(),
                entity.getLastExecutedAt(),
                entity.getNextExecuteAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    // ==================== 工具方法 ====================

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
