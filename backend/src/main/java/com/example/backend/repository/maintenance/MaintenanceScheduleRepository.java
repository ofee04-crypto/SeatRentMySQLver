package com.example.backend.repository.maintenance;

import com.example.backend.model.maintenance.MaintenanceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定期維護排程 Repository
 */
public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Integer> {

    /**
     * 查詢所有已到期且仍啟用的排程
     * 用於定時任務：找出 nextExecuteAt < now 且 isActive = true 的排程
     *
     * @param now 當前時間
     * @return 需要執行的排程清單
     */
    List<MaintenanceSchedule> findByNextExecuteAtBeforeAndIsActiveTrue(LocalDateTime now);

    /**
     * 依啟用狀態查詢排程
     *
     * @param isActive 是否啟用
     * @return 排程清單
     */
    List<MaintenanceSchedule> findByIsActive(Boolean isActive);

    /**
     * 依目標類型和目標 ID 查詢排程
     *
     * @param targetType 目標類型 (SPOT/SEAT)
     * @param targetId   目標 ID
     * @return 排程清單
     */
    List<MaintenanceSchedule> findByTargetTypeAndTargetId(String targetType, Integer targetId);

    /**
     * 依指派人員查詢排程
     *
     * @param assignedStaffId 人員 ID
     * @return 排程清單
     */
    List<MaintenanceSchedule> findByAssignedStaffId(Integer assignedStaffId);
}
