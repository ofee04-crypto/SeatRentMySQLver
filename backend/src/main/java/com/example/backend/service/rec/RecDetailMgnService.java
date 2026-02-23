package com.example.backend.service.rec;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.rec.DailyOrderCountDTO;
import com.example.backend.dto.rec.HourlyOrderCountDTO;
import com.example.backend.dto.rec.MonthlyOrderCountDTO;
import com.example.backend.dto.rec.OrderStatusStatsDTO;
import com.example.backend.dto.rec.RentalDurationDTO;
import com.example.backend.dto.rec.RentalDurationStatsDTO;
import com.example.backend.model.rec.RecRent;
import com.example.backend.model.rec.RentDetails;
import com.example.backend.repository.rec.RecRentRepository;
import com.example.backend.repository.rec.RentDetailsRepository;
import com.example.backend.repository.rec.RentDetailsSpecs;
import org.springframework.data.domain.Sort;

@Service
@Transactional
public class RecDetailMgnService {
    @Autowired
    private RentDetailsRepository detailRepos;
    @Autowired
    private RecRentRepository recRepos;

    public List<RentDetails> getAllRec() {
        return detailRepos.findAll(Sort.by(Sort.Direction.DESC, "recRentDT2"));
    }

    public List<RentDetails> search(
            Integer recSeqId,
            String recId,
            Integer memId,
            String memName,
            String recStatus,
            Integer spotId,
            String spotName,
            LocalDate startDate,
            LocalDate endDate) {

        Specification<RentDetails> spec = Specification.where(null);

        if (recId != null && !recId.isEmpty()) {
            spec = spec.and(RentDetailsSpecs.hasRecId(recId));
        }
        if (memId != null) {
            spec = spec.and(RentDetailsSpecs.hasMemId(memId));
        }
        if (memName != null && !memName.isEmpty()) {
            spec = spec.and(RentDetailsSpecs.memNameContains(memName));
        }
        if (recStatus != null && !recStatus.isEmpty()) {
            spec = spec.and(RentDetailsSpecs.hasRecStatus(recStatus));
        }
        if (spotId != null) {
            spec = spec.and(RentDetailsSpecs.hasSpotId(spotId));
        }
        if (spotName != null && !spotName.isEmpty()) {
            spec = spec.and(RentDetailsSpecs.spotNameContains(spotName));
        }
        // Time Range Search
        if (startDate != null || endDate != null) {
            spec = spec.and(RentDetailsSpecs.isWithinTimeRange(startDate, endDate));
        }

        return detailRepos.findAll(spec, Sort.by(Sort.Direction.DESC, "recRentDT2"));
    }

    public RentDetails getRecById(String recId) {
        // 找不到ID回傳ERR MSG
        return detailRepos.findById(recId).orElseThrow(
                () -> new RuntimeException("RentDetails not found with ID: " + recId));// ??
    }

    // 新增訂單
    public void insertOrUpdateRec(RecRent recRent) {
        recRepos.save(recRent);

    }

    // 呼叫Repository取得原始統計數據，並在服務層將其映射為DTO列表
    public List<MonthlyOrderCountDTO> getMonthlyOrderCounts(LocalDate startDate, LocalDate endDate) {
        // 將LocalDate轉換為LocalDateTime以符合Repository查詢需求
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Object[]> results = detailRepos.findMonthlyOrderCounts(startDateTime, endDateTime);
        return results.stream().map(row -> new MonthlyOrderCountDTO(
                ((Number) row[0]).intValue(), // 年份
                ((Number) row[1]).intValue(), // 月份
                ((Number) row[2]).longValue() // 訂單計數
        )).collect(Collectors.toList());
    }

    // 獲取訂單狀態統計數據（圓餅圖）
    public List<OrderStatusStatsDTO> getOrderStatusStats(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Object[]> results = detailRepos.findOrderStatusStats(startDateTime, endDateTime);
        return results.stream()
                .map(row -> new OrderStatusStatsDTO((String) row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    // 獲取訂單租借時長數據（散佈圖）
    public List<RentalDurationDTO> getRentalDurations(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Object[]> results = detailRepos.findRentalDurations(startDateTime, endDateTime);
        return results.stream().map(row -> {
            String recId = (String) row[0];
            LocalDateTime rentTime = (LocalDateTime) row[1];
            LocalDateTime returnTime = (LocalDateTime) row[2];
            // 計算租借時長（分鐘）
            long durationInMinutes = Duration.between(rentTime, returnTime).toMinutes();
            return new RentalDurationDTO(recId, rentTime, durationInMinutes);
        }).collect(Collectors.toList());
    }

    // 獲取每小時訂單統計
    public List<HourlyOrderCountDTO> getHourlyOrderCounts(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Object[]> results = detailRepos.findHourlyOrderCounts(startDateTime, endDateTime);
        return results.stream()
                .map(row -> new HourlyOrderCountDTO(((Number) row[0]).intValue(), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    // 獲取每日訂單統計
    public List<DailyOrderCountDTO> getDailyOrderCounts(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Object[]> results = detailRepos.findDailyOrderCounts(startDateTime, endDateTime);
        return results.stream()
                .map(row -> new DailyOrderCountDTO(row[0].toString(), ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    // 獲取租借時長統計 (30分鐘間隔)
    public List<RentalDurationStatsDTO> getRentalDurationStats(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Object[]> results = detailRepos.findRentalDurationStatsIn30MinIntervals(startDateTime, endDateTime);
        return results.stream()
                .map(row -> new RentalDurationStatsDTO(
                        row[0] != null ? ((Number) row[0]).intValue() : 0,
                        row[1] != null ? ((Number) row[1]).longValue() : 0L))
                .collect(Collectors.toList());
    }
}
