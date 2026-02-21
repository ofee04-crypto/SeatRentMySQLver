package com.example.backend.service.maintenance;

import com.example.backend.dto.maintenance.AssetStatsResponseDto;
import com.example.backend.model.maintenance.MaintenanceInformation;
import com.example.backend.model.spot.RentalSpot;
import com.example.backend.model.spot.Seat;
import com.example.backend.repository.maintenance.MaintenanceInformationRepository;
import com.example.backend.repository.spot.RentalSpotRepository;
import com.example.backend.repository.spot.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 資產健康度統計 Service
 * 提供機台(Spot)與椅子(Seat)在最近 7 天的維修/保養統計
 */
@Service
@Transactional(readOnly = true)
public class AssetStatsService {

    // 使用現有狀態常數
    private static final String STATUS_RESOLVED = MaintenanceInformationService.STATUS_RESOLVED;
    private static final String STATUS_CANCELLED = MaintenanceInformationService.STATUS_CANCELLED;
    private static final Set<String> DONE_STATUSES = Set.of(STATUS_RESOLVED, STATUS_CANCELLED);

    // 保養關鍵字
    private static final List<String> MAINTAIN_KEYWORDS = Arrays.asList("保養", "例行", "檢查", "巡檢", "點檢");

    // 時間窗口常數
    private static final int WINDOW_DAYS = 7;
    private static final long TOTAL_WINDOW_MINUTES = WINDOW_DAYS * 24L * 60L; // 10080

    private final MaintenanceInformationRepository ticketRepo;
    private final RentalSpotRepository spotRepo;
    private final SeatRepository seatRepo;

    public AssetStatsService(
            MaintenanceInformationRepository ticketRepo,
            RentalSpotRepository spotRepo,
            SeatRepository seatRepo
    ) {
        this.ticketRepo = ticketRepo;
        this.spotRepo = spotRepo;
        this.seatRepo = seatRepo;
    }

    /**
     * 取得機台(Spot)健康度統計
     */
    public List<AssetStatsResponseDto> getSpotStats() {
        LocalDateTime windowTo = LocalDateTime.now();
        LocalDateTime windowFrom = windowTo.minusDays(WINDOW_DAYS);

        // 取得所有工單
        List<MaintenanceInformation> allTickets = ticketRepo.findAll();

        // 篩選 spotId 不為空且與時間窗有重疊的工單
        List<MaintenanceInformation> spotTickets = allTickets.stream()
                .filter(t -> t.getSpotId() != null)
                .filter(t -> isOverlapping(t, windowFrom, windowTo))
                .collect(Collectors.toList());

        // 依 spotId 分組
        Map<Integer, List<MaintenanceInformation>> ticketsBySpot = spotTickets.stream()
                .collect(Collectors.groupingBy(MaintenanceInformation::getSpotId));

        // 一次查詢所有 spot 名稱
        Set<Integer> spotIds = ticketsBySpot.keySet();
        Map<Integer, String> spotNameMap = spotRepo.findAllById(spotIds).stream()
                .collect(Collectors.toMap(
                        RentalSpot::getSpotId,
                        s -> s.getSpotCode() + " - " + s.getSpotName()
                ));

        // 計算統計
        List<AssetStatsResponseDto> result = new ArrayList<>();
        for (Map.Entry<Integer, List<MaintenanceInformation>> entry : ticketsBySpot.entrySet()) {
            Integer spotId = entry.getKey();
            List<MaintenanceInformation> tickets = entry.getValue();
            String spotName = spotNameMap.getOrDefault(spotId, "未知資產#" + spotId);

            AssetStatsResponseDto dto = calculateStats("SPOT", spotId, spotName, tickets, windowFrom, windowTo);
            result.add(dto);
        }

        // 依維修次數降序排序
        result.sort((a, b) -> Integer.compare(b.getRepairCount(), a.getRepairCount()));

        return result;
    }

    /**
     * 取得椅子(Seat)健康度統計
     */
    public List<AssetStatsResponseDto> getSeatStats() {
        LocalDateTime windowTo = LocalDateTime.now();
        LocalDateTime windowFrom = windowTo.minusDays(WINDOW_DAYS);

        // 取得所有工單
        List<MaintenanceInformation> allTickets = ticketRepo.findAll();

        // 篩選 seatsId 不為空且與時間窗有重疊的工單
        List<MaintenanceInformation> seatTickets = allTickets.stream()
                .filter(t -> t.getSeatsId() != null)
                .filter(t -> isOverlapping(t, windowFrom, windowTo))
                .collect(Collectors.toList());

        // 依 seatsId 分組
        Map<Integer, List<MaintenanceInformation>> ticketsBySeat = seatTickets.stream()
                .collect(Collectors.groupingBy(MaintenanceInformation::getSeatsId));

        // 一次查詢所有 seat 名稱
        Set<Integer> seatIds = ticketsBySeat.keySet();
        Map<Integer, String> seatNameMap = seatRepo.findAllById(seatIds).stream()
                .collect(Collectors.toMap(
                        Seat::getSeatsId,
                        Seat::getSeatsName
                ));

        // 計算統計
        List<AssetStatsResponseDto> result = new ArrayList<>();
        for (Map.Entry<Integer, List<MaintenanceInformation>> entry : ticketsBySeat.entrySet()) {
            Integer seatId = entry.getKey();
            List<MaintenanceInformation> tickets = entry.getValue();
            String seatName = seatNameMap.getOrDefault(seatId, "未知資產#" + seatId);

            AssetStatsResponseDto dto = calculateStats("SEAT", seatId, seatName, tickets, windowFrom, windowTo);
            result.add(dto);
        }

        // 依維修次數降序排序
        result.sort((a, b) -> Integer.compare(b.getRepairCount(), a.getRepairCount()));

        return result;
    }

    /**
     * 計算單一資產的統計指標
     */
    private AssetStatsResponseDto calculateStats(
            String assetType,
            Integer assetId,
            String assetName,
            List<MaintenanceInformation> tickets,
            LocalDateTime windowFrom,
            LocalDateTime windowTo
    ) {
        int repairCount = 0;
        int maintainCount = 0;
        int repairDoneCount = 0;
        int maintainDoneCount = 0;
        int openCount = 0;
        long totalDowntimeMinutes = 0;

        for (MaintenanceInformation ticket : tickets) {
            boolean isMaintain = isMaintainTicket(ticket);
            boolean isDone = isDoneStatus(ticket.getIssueStatus());

            if (isMaintain) {
                maintainCount++;
                if (isDone) {
                    maintainDoneCount++;
                } else {
                    openCount++;
                }
            } else {
                repairCount++;
                if (isDone) {
                    repairDoneCount++;
                } else {
                    openCount++;
                }
            }

            // 計算停機時間（時間窗截斷）
            long overlapMinutes = calculateOverlapMinutes(ticket, windowFrom, windowTo);
            totalDowntimeMinutes += overlapMinutes;
        }

        // 計算指標
        double availability = 1.0 - ((double) totalDowntimeMinutes / TOTAL_WINDOW_MINUTES);
        availability = Math.max(0.0, Math.min(1.0, availability)); // clamp 0~1

        double failureRatePerDay = (double) repairCount / WINDOW_DAYS;

        double repairRate = (repairCount > 0) ? ((double) repairDoneCount / repairCount) : 0.0;

        return new AssetStatsResponseDto(
                assetType,
                assetId,
                assetName,
                repairCount,
                maintainCount,
                repairDoneCount,
                maintainDoneCount,
                openCount,
                totalDowntimeMinutes,
                availability,
                failureRatePerDay,
                repairRate,
                windowFrom,
                windowTo
        );
    }

    /**
     * 判斷工單是否為保養類型
     */
    private boolean isMaintainTicket(MaintenanceInformation ticket) {
        String issueType = ticket.getIssueType();
        String issueDesc = ticket.getIssueDesc();

        for (String keyword : MAINTAIN_KEYWORDS) {
            if (issueType != null && issueType.contains(keyword)) {
                return true;
            }
            if (issueDesc != null && issueDesc.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判斷狀態是否為已結案
     */
    private boolean isDoneStatus(String status) {
        return status != null && DONE_STATUSES.contains(status);
    }

    /**
     * 判斷工單是否與時間窗有重疊
     */
    private boolean isOverlapping(MaintenanceInformation ticket, LocalDateTime windowFrom, LocalDateTime windowTo) {
        LocalDateTime ticketStart = getTicketStart(ticket);
        LocalDateTime ticketEnd = getTicketEnd(ticket, windowTo);

        // 有重疊：ticketEnd >= windowFrom && ticketStart <= windowTo
        return !ticketEnd.isBefore(windowFrom) && !ticketStart.isAfter(windowTo);
    }

    /**
     * 計算工單在時間窗內的停機分鐘數
     */
    private long calculateOverlapMinutes(MaintenanceInformation ticket, LocalDateTime windowFrom, LocalDateTime windowTo) {
        LocalDateTime ticketStart = getTicketStart(ticket);
        LocalDateTime ticketEnd = getTicketEnd(ticket, windowTo);

        // 截斷到時間窗內
        LocalDateTime effectiveStart = ticketStart.isBefore(windowFrom) ? windowFrom : ticketStart;
        LocalDateTime effectiveEnd = ticketEnd.isAfter(windowTo) ? windowTo : ticketEnd;

        // 計算重疊分鐘數
        if (effectiveEnd.isAfter(effectiveStart)) {
            return ChronoUnit.MINUTES.between(effectiveStart, effectiveEnd);
        }
        return 0;
    }

    /**
     * 取得工單開始時間 (優先使用 startAt，否則用 reportedAt)
     */
    private LocalDateTime getTicketStart(MaintenanceInformation ticket) {
        if (ticket.getStartAt() != null) {
            return ticket.getStartAt();
        }
        return ticket.getReportedAt() != null ? ticket.getReportedAt() : LocalDateTime.now();
    }

    /**
     * 取得工單結束時間 (優先使用 resolvedAt，否則用 windowTo 作為進行中工單的結束時間)
     */
    private LocalDateTime getTicketEnd(MaintenanceInformation ticket, LocalDateTime windowTo) {
        if (ticket.getResolvedAt() != null) {
            return ticket.getResolvedAt();
        }
        // 未結案工單，以當前時間為結束（但不超過 windowTo）
        return windowTo;
    }
}
