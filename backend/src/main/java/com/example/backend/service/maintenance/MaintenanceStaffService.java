package com.example.backend.service.maintenance;

import com.example.backend.dto.maintenance.MaintenanceStaffResponseDto;
import com.example.backend.model.maintenance.MaintenanceInformation;
import com.example.backend.model.maintenance.MaintenanceStaff;
import com.example.backend.repository.maintenance.MaintenanceInformationRepository;
import com.example.backend.repository.maintenance.MaintenanceStaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MaintenanceStaffService {

    private final MaintenanceStaffRepository staffRepo;
    private final MaintenanceInformationRepository mtifRepo;
    private final MaintenanceInformationService mtifService;

    public MaintenanceStaffService(MaintenanceStaffRepository staffRepo,
                                   MaintenanceInformationRepository mtifRepo,
                                   MaintenanceInformationService mtifService) {
        this.staffRepo = staffRepo;
        this.mtifRepo = mtifRepo;
        this.mtifService = mtifService;
    }

    /**
     * 轉移工單並刪除人員
     * targetStaffId: 接手的人
     * deleteStaffId: 要停用的人
     * operator: 執行此操作的管理員名稱
     */
    public void transferAndDelete(Integer targetStaffId, Integer deleteStaffId, String operator) {
        if (deleteStaffId == null || targetStaffId == null) {
            throw new IllegalArgumentException("ID 不能為空");
        }

        MaintenanceStaff targetStaff = getRequiredStaff(targetStaffId);
        MaintenanceStaff deleteStaff = getRequiredStaff(deleteStaffId);

        // ★ 呼叫 transferTickets 並傳入操作者名稱
        mtifService.transferTickets(deleteStaffId, targetStaffId, operator);

        deleteStaff.setIsActive(false);
        staffRepo.save(deleteStaff);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // 3. 新增維護人員
    public MaintenanceStaffResponseDto createStaff(MaintenanceStaff staff) {
        validateForCreate(staff);
        MaintenanceStaff entity = new MaintenanceStaff();
        entity.setStaffName(staff.getStaffName().trim());
        entity.setStaffCompany(isBlank(staff.getStaffCompany()) ? null : staff.getStaffCompany().trim());
        entity.setStaffEmail(isBlank(staff.getStaffEmail()) ? null : staff.getStaffEmail().trim());
        entity.setStaffPhone(isBlank(staff.getStaffPhone()) ? null : staff.getStaffPhone().trim());
        entity.setStaffNote(isBlank(staff.getStaffNote()) ? null : staff.getStaffNote().trim());
        
        if (staff.getIsActive() != null) {
            entity.setIsActive(staff.getIsActive());
        }

        MaintenanceStaff saved = staffRepo.save(entity);
        return maintenanceStaffResponseDto(saved);
    }

    // 更新維護人員資料
    public MaintenanceStaffResponseDto updateStaff(Integer id, MaintenanceStaff staff) {
        if (id == null) throw new IllegalArgumentException("ID 不能為 null");
        
        staff.setStaffId(id);
        validateForUpdate(staff);

        MaintenanceStaff existing = getRequiredStaff(id);

        existing.setStaffName(staff.getStaffName().trim());
        existing.setStaffCompany(isBlank(staff.getStaffCompany()) ? null : staff.getStaffCompany().trim());
        existing.setStaffPhone(isBlank(staff.getStaffPhone()) ? null : staff.getStaffPhone().trim());
        existing.setStaffNote(isBlank(staff.getStaffNote()) ? null : staff.getStaffNote().trim());
        existing.setStaffEmail(isBlank(staff.getStaffEmail()) ? null : staff.getStaffEmail().trim());
        
        if (staff.getIsActive() != null) {
            existing.setIsActive(staff.getIsActive());
        }

        MaintenanceStaff saved = staffRepo.save(existing);
        return maintenanceStaffResponseDto(saved);
    }

    // 取得啟用中的人員 (供下拉選單使用)
    public List<MaintenanceStaffResponseDto> getActiveStaff() {
        List<MaintenanceStaff> activeStaff = staffRepo.findByIsActive(true);
        return activeStaff.stream()
                .map(this::maintenanceStaffResponseDto)
                .collect(Collectors.toList());
    }

    // 軟刪除 (若無關聯工單)
    public void deleteStaff(int staffId) {
        MaintenanceStaff existing = getRequiredStaff(staffId);
        if (Boolean.FALSE.equals(existing.getIsActive())) {
            return;
        }

        // 定義那些狀態是工作中: 已指派或是維修中
        List<String> activeStatuses = Arrays.asList("ASSIGNED", "UNDER_MAINTENANCE");
        // 檢查該人員是否有工作中的工單
        boolean hasActiveWork = mtifRepo.existsByAssignedStaffIdAndIssueStatusIn(staffId, activeStatuses);
        
        if (hasActiveWork) {
            throw new IllegalStateException("該人員尚有未完成的維修工單，無法刪除！請先轉派工單。");
        }
        
        existing.setIsActive(false);
        staffRepo.save(existing);
    }

    // 2. 取得單一維護人員
    public MaintenanceStaffResponseDto getStaffById(Integer id) {
        MaintenanceStaff staff = staffRepo.findById(id).orElse(null);
        return maintenanceStaffResponseDto(staff);
    }

    @Transactional(readOnly = true)
    public MaintenanceStaff getRequiredStaff(int staffId) {
        return staffRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的維護人員，staffId = " + staffId));
    }

    // 1. 取得所有維護人員
    public List<MaintenanceStaffResponseDto> getAllStaff() {
        List<MaintenanceStaff> staffList = staffRepo.findAll();
        return staffList.stream()
                .map(this::maintenanceStaffResponseDto) 
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MaintenanceStaff> getInactiveStaff() {
        return staffRepo.findByIsActiveFalseOrderByStaffIdAsc();
    }

    @Transactional(readOnly = true)
    public List<MaintenanceStaff> getStaffByCompany(String staffCompany) {
        if (isBlank(staffCompany)) throw new IllegalArgumentException("廠商名稱不能為空白");
        return staffRepo.findByStaffCompany(staffCompany.trim());
    }

    private void validateForCreate(MaintenanceStaff staff) {
        if (staff == null) throw new IllegalArgumentException("維護人員資料不能為 null");
        if (isBlank(staff.getStaffName())) throw new IllegalArgumentException("維護人員姓名必填");

        if (!isBlank(staff.getStaffEmail())) {
            String email = staff.getStaffEmail().trim();
            if (staffRepo.existsByStaffEmailIgnoreCase(email)) {
                throw new IllegalArgumentException("此 Email 已被使用");
            }
        }
    }

    private void validateForUpdate(MaintenanceStaff staff) {
        if (staff == null) throw new IllegalArgumentException("資料不能為 null");
        if (staff.getStaffId() == null) throw new IllegalArgumentException("ID 不能為 null");
        if (isBlank(staff.getStaffName())) throw new IllegalArgumentException("姓名必填");
        
        if (!isBlank(staff.getStaffEmail())) {
            String email = staff.getStaffEmail().trim();
            MaintenanceStaff existing = staffRepo.findByStaffEmailIgnoreCase(email);
            if (existing != null && !existing.getStaffId().equals(staff.getStaffId())) {
                throw new IllegalArgumentException("此 Email 已被其他維護人員使用");
            }
        }
    }

    private MaintenanceStaffResponseDto maintenanceStaffResponseDto(MaintenanceStaff staff){
        if(staff == null) return null;
        return new MaintenanceStaffResponseDto(
            staff.getStaffId(),
            staff.getStaffName(),
            staff.getStaffCompany(),
            staff.getStaffEmail(),
            staff.getStaffPhone(),
            staff.getStaffNote(),
            staff.getIsActive(),
            staff.getCreatedAt()
        );
    }
}