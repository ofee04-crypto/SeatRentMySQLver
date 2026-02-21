package com.example.backend.repository.maintenance;

import com.example.backend.model.maintenance.MaintenanceInformation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceInformationRepository extends JpaRepository<MaintenanceInformation, Integer> {

    List<MaintenanceInformation> findBySpotIdOrderByReportedAtDescTicketIdAsc(Integer spotId);

    // 用於狀態查詢
    List<MaintenanceInformation> findByIssueStatusOrderByReportedAtDescTicketIdAsc(String issueStatus);

    // 用於 In List 查詢 (Active / History)
    List<MaintenanceInformation> findByIssueStatusInOrderByReportedAtDescTicketIdAsc(List<String> issueStatusList);

    // 查詢全部並排序 (解決 getAllTickets 沒排序的問題)
    List<MaintenanceInformation> findAllByOrderByReportedAtDescTicketIdAsc();

    //讓我們可以查某張椅子的所有工單
    List<MaintenanceInformation> findBySeatsIdOrderByReportedAtDescTicketIdAsc(Integer seatsId);

    //用於轉移工單
    // 在 interface 裡面新增這行
List<MaintenanceInformation> findByAssignedStaffIdAndIssueStatusIn(Integer assignedStaffId, List<String> issueStatuses);

boolean existsByAssignedStaffIdAndIssueStatusIn(Integer assignedStaffId, List<String> statuses);

    // ========== 新增：Task 2 歷史工單查詢支援 ==========
    
    /**
     * 查詢指定人員的工單（依狀態篩選）
     */
    List<MaintenanceInformation> findByAssignedStaffIdOrderByReportedAtDescTicketIdAsc(Integer assignedStaffId);
    
    /**
     * 查詢指定人員的特定狀態工單
     */
    List<MaintenanceInformation> findByAssignedStaffIdAndIssueStatusInOrderByReportedAtDescTicketIdAsc(Integer assignedStaffId, List<String> issueStatuses);

    // ========== 防爆單檢查 (排程自動開單用) ==========
    
    /**
     * 檢查某機台是否有未結案工單 (SPOT)
     */
    boolean existsBySpotIdAndSeatsIdIsNullAndIssueStatusIn(Integer spotId, List<String> statuses);

    /**
     * 檢查某椅子是否有未結案工單 (SEAT)
     */
    boolean existsBySeatsIdAndIssueStatusIn(Integer seatsId, List<String> statuses);

    
}
