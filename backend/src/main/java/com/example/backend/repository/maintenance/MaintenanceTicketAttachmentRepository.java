package com.example.backend.repository.maintenance;

import com.example.backend.model.maintenance.MaintenanceTicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 維修工單附件 Repository
 */
@Repository
public interface MaintenanceTicketAttachmentRepository extends JpaRepository<MaintenanceTicketAttachment, Integer> {

    /**
     * 查詢指定工單的所有啟用附件
     * 排序：sortOrder ASC, createdAt DESC
     */
    List<MaintenanceTicketAttachment> findByTicketIdAndIsActiveTrueOrderBySortOrderAscCreatedAtDesc(Integer ticketId);

    /**
     * 查詢指定工單的最大 sortOrder（用於新增時計算下一個順序）
     */
    @Query("SELECT COALESCE(MAX(a.sortOrder), 0) FROM MaintenanceTicketAttachment a WHERE a.ticketId = :ticketId")
    Integer findMaxSortOrderByTicketId(@Param("ticketId") Integer ticketId);

    /**
     * 檢查 storedName 是否已存在於該工單（防止重複）
     */
    boolean existsByTicketIdAndStoredName(Integer ticketId, String storedName);
}
