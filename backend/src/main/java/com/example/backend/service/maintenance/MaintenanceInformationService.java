package com.example.backend.service.maintenance;

import com.example.backend.dto.maintenance.SpotOptionDto;
import com.example.backend.dto.maintenance.MaintenanceLogResponseDto;
import com.example.backend.model.maintenance.MaintenanceInformation;
import com.example.backend.model.maintenance.MaintenanceStaff;
import com.example.backend.model.spot.RentalSpot;
import com.example.backend.repository.maintenance.MaintenanceInformationRepository;
import com.example.backend.repository.maintenance.MaintenanceStaffRepository;
import com.example.backend.repository.spot.RentalSpotRepository;
import com.example.backend.repository.spot.SeatRepository; 
import com.example.backend.model.spot.Seat;
import com.example.backend.model.maintenance.MaintenanceLog;
import com.example.backend.repository.maintenance.MaintenanceLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class MaintenanceInformationService {

    // 狀態常數
    public static final String STATUS_REPORTED = "REPORTED";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_UNDER_MAINTENANCE = "UNDER_MAINTENANCE";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_URGENT = "URGENT";

    public static final String RESULT_FIXED = "FIXED";
    public static final String RESULT_MAINTAINED = "MAINTAINED"; //  新增：已保養
    public static final String RESULT_NOT_FIXED = "NOT_FIXED";
    public static final String RESULT_NO_ISSUE = "NO_ISSUE";
    public static final String RESULT_UNFIXABLE = "UNFIXABLE"; //  統一為 UNFIXABLE
    public static final String RESULT_OTHER = "OTHER";

    public static final String SPOT_STATUS_OPERATIONAL = "營運中";
    public static final String SPOT_STATUS_MAINTENANCE = "維修中";
    public static final String SEAT_STATUS_ENABLED = "啟用";
    public static final String SEAT_STATUS_REPAIRING = "維修中";
    
    private final MaintenanceInformationRepository mtifRepo;
    private final MaintenanceStaffRepository staffRepo;
    private final RentalSpotRepository rentalSpotRepo;
    private final SeatRepository seatRepo;
    private final MaintenanceLogRepository logRepo;

    public MaintenanceInformationService(MaintenanceInformationRepository mtifRepo,
                                         MaintenanceStaffRepository staffRepo,
                                         RentalSpotRepository rentalSpotRepo,
                                         SeatRepository seatRepo,
                                         MaintenanceLogRepository logRepo) {
        this.mtifRepo = mtifRepo;
        this.staffRepo = staffRepo;
        this.rentalSpotRepo = rentalSpotRepo;
        this.seatRepo = seatRepo;
        this.logRepo = logRepo;
    }

    public List<Seat> getAllSeats(){ return seatRepo.findAll(); }
    public List<Seat> getSeatsBySpot(Integer spotId){ return seatRepo.findBySpotId(spotId); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    @Transactional(readOnly = true)
    public List<Integer> getAllSpotIds(){
        return rentalSpotRepo.findAll().stream().map(RentalSpot::getSpotId).sorted().toList();
    }

    public List<SpotOptionDto> getSpotOptions(){
        return rentalSpotRepo.findAll().stream()
                .map(spot -> {
                    //  修正原因：前端地圖功能需要座標資料，將 RentalSpot 的 BigDecimal 轉為 Double 傳給前端
                    // 使用新建構子傳入 latitude/longitude，若為 null 則保持 null（前端會判斷）
                    Double lat = (spot.getLatitude() != null) ? spot.getLatitude().doubleValue() : null;
                    Double lng = (spot.getLongitude() != null) ? spot.getLongitude().doubleValue() : null;
                    
                    return new SpotOptionDto(
                        spot.getSpotId(), 
                        spot.getSpotCode(), 
                        spot.getSpotName(), 
                        spot.getSpotAddress(), 
                        spot.getSpotStatus(),
                        lat,  //  新增：經度（前端地圖用）
                        lng   //  新增：緯度（前端地圖用）
                    );
                })
                .sorted(Comparator.comparingInt(SpotOptionDto::getSpotId)).toList();
    }

    // ============ 建立 / 更新 / 刪除 ============

    public MaintenanceInformation createTicket(MaintenanceInformation mtif, String operator) {
        mtif.setTicketId(null);
        validateForCreate(mtif);
        validateAssignedStaff(mtif.getAssignedStaffId());

        mtif.setIssueType(mtif.getIssueType().trim());
        mtif.setIssueDesc(isBlank(mtif.getIssueDesc()) ? null : mtif.getIssueDesc().trim());

        if (isBlank(mtif.getIssuePriority())) {
            mtif.setIssuePriority(PRIORITY_NORMAL);
        } else {
            String p = mtif.getIssuePriority().trim().toUpperCase();
            if (!isValidPriority(p)) throw new IllegalArgumentException("無效的優先權: " + p);
            mtif.setIssuePriority(p);
        }

        mtif.setIssueStatus(mtif.getAssignedStaffId() != null ? STATUS_ASSIGNED : STATUS_REPORTED);
        mtif.setStartAt(null);
        mtif.setResolvedAt(null);
        mtif.setResultType(null);
        mtif.setResolveNote(null);

        MaintenanceInformation saved = mtifRepo.save(mtif);
        
        String finalOperator = isBlank(operator) ? "系統管理員" : operator;
        String comment = String.format("建立工單 | 類型: %s | 優先權: %s", saved.getIssueType(), saved.getIssuePriority());
        saveLog(saved, finalOperator, "CREATED", comment);

        if (saved.getAssignedStaffId() != null) {
            String staffName = staffRepo.findById(saved.getAssignedStaffId())
                    .map(MaintenanceStaff::getStaffName).orElse("未知人員");
            saveLog(saved, finalOperator, "ASSIGNED", "指派負責人: " + staffName);
        }
        
        return saved;
    }

    public MaintenanceInformation updateTicket(MaintenanceInformation mtif, String operator) {
        if (mtif.getTicketId() == null) throw new IllegalArgumentException("更新工單時 ticketId 為必填欄位");
        if (isBlank(mtif.getIssueType())) throw new IllegalArgumentException("issueType 為必填欄位");
        MaintenanceInformation existing = getRequiredTicket(mtif.getTicketId());

        String currentStatus = existing.getIssueStatus();
        if (STATUS_RESOLVED.equals(currentStatus) || STATUS_CANCELLED.equals(currentStatus)) {
            throw new IllegalStateException("工單已結案或取消，不允許修改內容");
        }
        
        Integer oldStaffId = existing.getAssignedStaffId();
        Integer newStaffId = mtif.getAssignedStaffId();
        boolean staffChanged = !java.util.Objects.equals(oldStaffId, newStaffId); 

        existing.setIssueType(mtif.getIssueType().trim());
        existing.setIssueDesc(isBlank(mtif.getIssueDesc()) ? null : mtif.getIssueDesc().trim());
        
        if (!isBlank(mtif.getIssuePriority())) {
            String p = mtif.getIssuePriority().trim().toUpperCase();
            if (!isValidPriority(p)) throw new IllegalArgumentException("無效的優先權: " + p);
            existing.setIssuePriority(p);
        }

        if (newStaffId != null) {
            validateAssignedStaff(newStaffId);
            existing.setAssignedStaffId(newStaffId);
        }

        MaintenanceInformation saved = mtifRepo.save(existing);

        if (staffChanged && newStaffId != null) {
            String staffName = staffRepo.findById(newStaffId)
                    .map(MaintenanceStaff::getStaffName).orElse("未知");

            if (STATUS_REPORTED.equals(saved.getIssueStatus())) {
                saved.setIssueStatus(STATUS_ASSIGNED);
                saved = mtifRepo.save(saved);
            }

            // ★ 智慧判斷移轉：如果原本有人 (old != null) 且不是新的人 -> 移轉
            String action = (oldStaffId != null) ? "TRANSFERRED" : "ASSIGNED";
            String comment = (oldStaffId != null) ? ("工單移轉 | 負責人變更為: " + staffName) : ("指派負責人: " + staffName);
            
            saveLog(saved, isBlank(operator) ? "系統管理員" : operator, action, comment);
        }

        return saved;
    }

    public void deleteTicket(int ticketId) {
        if (!mtifRepo.existsById(ticketId)) throw new IllegalArgumentException("找不到工單 " + ticketId);
        mtifRepo.deleteById(ticketId);
    }

    public void cancelTicket(int ticketId, String cancelReason, String operator) {
        MaintenanceInformation mtif = getRequiredTicket(ticketId);
        String status = mtif.getIssueStatus();

        if (STATUS_UNDER_MAINTENANCE.equals(status)) throw new IllegalStateException("維修中不可取消工單");
        if (STATUS_RESOLVED.equals(status)) throw new IllegalStateException("工單已結案，無法取消");
        if (STATUS_CANCELLED.equals(status)) throw new IllegalStateException("工單已是取消狀態");

        mtif.setIssueStatus(STATUS_CANCELLED);
        if (!isBlank(cancelReason)) {
            String oldNote = mtif.getResolveNote();
            mtif.setResolveNote(isBlank(oldNote) ? "[取消原因] " + cancelReason : oldNote + "；[取消原因] " + cancelReason);
        }

        mtifRepo.save(mtif);
        saveLog(mtif, isBlank(operator) ? "系統管理員" : operator, "CANCELLED", "取消原因: " + (isBlank(cancelReason) ? "未提供" : cancelReason));
    }

    @Transactional(readOnly = true)
    public MaintenanceInformation getTicketById(int ticketId) { return mtifRepo.findById(ticketId).orElse(null); }

    @Transactional(readOnly = true)
    public MaintenanceInformation getRequiredTicket(int ticketId) {
        return mtifRepo.findById(ticketId).orElseThrow(() -> new IllegalArgumentException("找不到工單 " + ticketId));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceInformation> getTicketsBySpotId(int spotId) { return mtifRepo.findBySpotIdOrderByReportedAtDescTicketIdAsc(spotId); }

    @Transactional(readOnly = true)
    public List<MaintenanceInformation> getAllTickets() { return mtifRepo.findAllByOrderByReportedAtDescTicketIdAsc(); }

    @Transactional(readOnly = true)
    public List<MaintenanceInformation> getActiveTickets() {
        return mtifRepo.findByIssueStatusInOrderByReportedAtDescTicketIdAsc(Arrays.asList(STATUS_REPORTED, STATUS_ASSIGNED, STATUS_UNDER_MAINTENANCE));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceInformation> getHistoryTickets() {
        return mtifRepo.findByIssueStatusInOrderByReportedAtDescTicketIdAsc(Arrays.asList(STATUS_RESOLVED, STATUS_CANCELLED));
    }

    // ★ assignStaff 接收 operator
    public void assignStaff(int ticketId, Integer staffId, String operator) {
        MaintenanceInformation mtif = getRequiredTicket(ticketId);
        String currentStatus = mtif.getIssueStatus();

        if (STATUS_RESOLVED.equals(currentStatus) || STATUS_CANCELLED.equals(currentStatus)) {
            throw new IllegalStateException("目前狀態無法變更指派");
        }

        Integer oldStaffId = mtif.getAssignedStaffId();
        validateAssignedStaff(staffId);
        mtif.setAssignedStaffId(staffId);

        if (STATUS_REPORTED.equals(currentStatus) && staffId != null) {
            mtif.setIssueStatus(STATUS_ASSIGNED);
        } else if (staffId == null && STATUS_ASSIGNED.equals(currentStatus)) {
            mtif.setIssueStatus(STATUS_REPORTED);
        }
        
        mtifRepo.save(mtif);
        
        if (staffId != null) {
            MaintenanceStaff newStaff = staffRepo.findById(staffId).orElse(null);
            String newStaffName = (newStaff != null) ? newStaff.getStaffName() : "未知";
            
            // ★ 判斷邏輯：原本有人 != 新的人 => 移轉
            String action = (oldStaffId != null && !oldStaffId.equals(staffId)) ? "TRANSFERRED" : "ASSIGNED";
            String comment = (oldStaffId != null && !oldStaffId.equals(staffId)) 
                    ? ("工單移轉 | 負責人變更為: " + newStaffName) 
                    : ("指派負責人: " + newStaffName);

            saveLog(mtif, isBlank(operator) ? "系統管理員" : operator, action, comment);
        }
    }

    // ★ transferTickets 確保接收 operator 並傳給 assignStaff
    // 注意：原本您的版本這裡可能漏了 operator，導致無法紀錄是誰轉移的
    public void transferTickets(Integer fromStaffId, Integer toStaffId, String operator) {
        if (fromStaffId == null || toStaffId == null) return;
        List<MaintenanceInformation> tickets = mtifRepo.findByAssignedStaffIdAndIssueStatusIn(
            fromStaffId, Arrays.asList(STATUS_ASSIGNED, STATUS_UNDER_MAINTENANCE));
        
        for (MaintenanceInformation ticket : tickets) {
            try {
                // 呼叫 assignStaff，它已經包含 "TRANSFERRED" 的判斷邏輯
                assignStaff(ticket.getTicketId(), toStaffId, operator);
            } catch (Exception e) {
                System.err.println("工單移轉失敗 ID: " + ticket.getTicketId());
            }
        }
    }

    public void startTicket(int ticketId) {
        MaintenanceInformation mtif = getRequiredTicket(ticketId);
        String status = mtif.getIssueStatus();

        if (!STATUS_REPORTED.equals(status) && !STATUS_ASSIGNED.equals(status)) {
            throw new IllegalStateException("目前狀態無法開始維修");
        }
        if (mtif.getAssignedStaffId() == null) throw new IllegalStateException("尚未指派人員");

        if (mtif.getStartAt() == null) mtif.setStartAt(LocalDateTime.now());
        mtif.setIssueStatus(STATUS_UNDER_MAINTENANCE);

        if (mtif.getSpotId() != null) {
            RentalSpot spot = rentalSpotRepo.findById(mtif.getSpotId()).orElseThrow();
            spot.setSpotStatus(SPOT_STATUS_MAINTENANCE);
            rentalSpotRepo.save(spot);
        }
        if (mtif.getSeatsId() != null) {
            Seat seat = this.seatRepo.findById(mtif.getSeatsId()).orElseThrow();
            seat.setSeatsStatus(SEAT_STATUS_REPAIRING);
            this.seatRepo.save(seat);
        }
        mtifRepo.save(mtif);
        
        MaintenanceStaff staff = staffRepo.findById(mtif.getAssignedStaffId()).orElse(null);
        String staffName = (staff != null) ? staff.getStaffName() : "未知";
        saveLog(mtif, staffName, "STARTED", "開始進行維修作業");
    }

    public void resolveTicket(int ticketId, String resultType, String resolveNote) {
        if (isBlank(resultType)) throw new IllegalArgumentException("維修結果必填");
        if (!isValidResultType(resultType)) throw new IllegalArgumentException("錯誤結果類型");

        MaintenanceInformation mtif = getRequiredTicket(ticketId);
        if (!STATUS_UNDER_MAINTENANCE.equals(mtif.getIssueStatus())) throw new IllegalStateException("只有維修中工單可結案");

        mtif.setResolvedAt(LocalDateTime.now());
        mtif.setResultType(resultType);
        mtif.setResolveNote(resolveNote);
        mtif.setIssueStatus(STATUS_RESOLVED);
        mtifRepo.save(mtif); 
        
        MaintenanceStaff staff = staffRepo.findById(mtif.getAssignedStaffId()).orElse(null);
        String staffName = (staff != null) ? staff.getStaffName() : "未知";
        saveLog(mtif, staffName, "RESOLVED", String.format("維修完成 | 結果: %s | 備註: %s", resultType, resolveNote));

        List<String> activeStatuses = Arrays.asList(STATUS_REPORTED, STATUS_ASSIGNED, STATUS_UNDER_MAINTENANCE);

        if (mtif.getSpotId() != null) {
            List<MaintenanceInformation> spotTickets = getTicketsBySpotId(mtif.getSpotId());
            boolean hasOther = spotTickets.stream().filter(t -> !t.getTicketId().equals(ticketId))
                    .anyMatch(t -> activeStatuses.contains(t.getIssueStatus()));
            if (!hasOther) {
                RentalSpot spot = rentalSpotRepo.findById(mtif.getSpotId()).orElseThrow();
                spot.setSpotStatus(SPOT_STATUS_OPERATIONAL);
                rentalSpotRepo.save(spot);
            }
        }
        if (mtif.getSeatsId() != null) {
            List<MaintenanceInformation> seatTickets = mtifRepo.findBySeatsIdOrderByReportedAtDescTicketIdAsc(mtif.getSeatsId());
            boolean hasOther = seatTickets.stream().filter(t -> !t.getTicketId().equals(ticketId))
                    .anyMatch(t -> activeStatuses.contains(t.getIssueStatus()));
            if (!hasOther) {
                Seat seat = this.seatRepo.findById(mtif.getSeatsId()).orElseThrow();
                seat.setSeatsStatus(SEAT_STATUS_ENABLED);
                this.seatRepo.save(seat);
            }
        }
    }
    
    private void validateForCreate(MaintenanceInformation mtif) {
        if (mtif == null) throw new IllegalArgumentException("工單空白");
        boolean hasSpot = (mtif.getSpotId() != null && mtif.getSpotId() > 0);
        boolean hasSeat = (mtif.getSeatsId() != null && mtif.getSeatsId() > 0);
        if (!hasSpot && !hasSeat) throw new IllegalArgumentException("需指定場地或椅子");
        if (isBlank(mtif.getIssueType())) throw new IllegalArgumentException("issueType 必填");
        if (hasSpot && !rentalSpotRepo.existsById(mtif.getSpotId())) throw new IllegalArgumentException("租借點不存在");
        if (hasSeat && !seatRepo.existsById(mtif.getSeatsId())) throw new IllegalArgumentException("椅子不存在");
    }

    private void validateAssignedStaff(Integer staffId) {
        if (staffId == null) return;
        MaintenanceStaff staff = staffRepo.findById(staffId).orElse(null);
        if (staff == null) throw new IllegalArgumentException("找不到人員");
        if (Boolean.FALSE.equals(staff.getIsActive())) throw new IllegalArgumentException("該人員已停用");
    }

    private boolean isValidResultType(String resultType) {
        // ✅ 支持 FIXED, MAINTAINED, NOT_FIXED, NO_ISSUE, UNFIXABLE, OTHER
        return !isBlank(resultType) && Arrays.asList(
            RESULT_FIXED, 
            RESULT_MAINTAINED, 
            RESULT_UNFIXABLE, 
            RESULT_OTHER
        ).contains(resultType);
    }

    private boolean isValidPriority(String priority) {
        return !isBlank(priority) && Arrays.asList(PRIORITY_LOW, PRIORITY_NORMAL, PRIORITY_HIGH, PRIORITY_URGENT).contains(priority);
    }

    private void saveLog(MaintenanceInformation ticket, String operator, String action, String comment) {
        if (ticket == null || isBlank(operator) || isBlank(action)) return;
        logRepo.save(new MaintenanceLog(ticket, operator.trim(), action.trim(), comment));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceLogResponseDto> getTicketLogs(Integer ticketId) {
        if (ticketId == null) throw new IllegalArgumentException("ticketId 空");
        if (!mtifRepo.existsById(ticketId)) throw new IllegalArgumentException("找不到工單");
        
        return logRepo.findByTicketTicketIdOrderByCreatedAtDesc(ticketId).stream()
                   .map(log -> new MaintenanceLogResponseDto(log.getLogId(), log.getOperator(), log.getAction(), log.getComment(), log.getCreatedAt()))
                   .toList();
    }
    
    public List<MaintenanceInformation> getTicketsByStaff(Integer staffId, List<String> statuses) {
        if (statuses != null && !statuses.isEmpty()) {
            return mtifRepo.findByAssignedStaffIdAndIssueStatusInOrderByReportedAtDescTicketIdAsc(staffId, statuses);
        } else {
            return mtifRepo.findByAssignedStaffIdOrderByReportedAtDescTicketIdAsc(staffId);
        }
    }
}