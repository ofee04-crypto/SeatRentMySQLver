package com.example.backend.repository.maintenance;

import com.example.backend.model.maintenance.MaintenanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 維修歷程 Repository
 */
@Repository
public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Integer> {

    /**
     * 查詢指定工單的所有歷程記錄，按時間倒序排列 (最新的在前)
     * 方法命名規則：findBy + 關聯物件 + 關聯物件欄位 + OrderBy + 排序欄位 + 排序方向
     */
    List<MaintenanceLog> findByTicketTicketIdOrderByCreatedAtDesc(Integer ticketId);
}
