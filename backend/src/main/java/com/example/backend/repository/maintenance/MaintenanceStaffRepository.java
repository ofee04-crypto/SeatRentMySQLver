package com.example.backend.repository.maintenance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.model.maintenance.MaintenanceStaff;

public interface MaintenanceStaffRepository extends JpaRepository<MaintenanceStaff, Integer> {

    // 檢查 email 是否存在 (忽略大小寫) - 用於 create
    boolean existsByStaffEmailIgnoreCase(String staffEmail);

    // 查詢 email (忽略大小寫) - 用於 update 檢查
    MaintenanceStaff findByStaffEmailIgnoreCase(String staffEmail);

    MaintenanceStaff findByStaffEmail(String staffEmail);

    List<MaintenanceStaff> findByStaffCompany(String staffCompany);

    // 修復：根據啟用狀態查詢人員
    List<MaintenanceStaff> findByIsActive(Boolean isActive);

    List<MaintenanceStaff> findByIsActiveTrueOrderByStaffIdAsc();

    List<MaintenanceStaff> findByIsActiveFalseOrderByStaffIdAsc();
    
}
