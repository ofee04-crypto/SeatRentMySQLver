package com.example.backend.controller.maintenance;

import com.example.backend.dto.maintenance.AttachmentResponseDto;
import com.example.backend.dto.maintenance.SpotOptionDto;
import com.example.backend.dto.maintenance.MaintenanceLogResponseDto;
import com.example.backend.dto.maintenance.MaintenanceStaffResponseDto;
import com.example.backend.model.maintenance.MaintenanceInformation;
import com.example.backend.model.maintenance.MaintenanceStaff;
import com.example.backend.model.member.Admin;
import com.example.backend.model.spot.Seat;
import com.example.backend.service.maintenance.AttachmentService;
import com.example.backend.service.maintenance.MaintenanceInformationService;
import com.example.backend.service.maintenance.MaintenanceStaffService;
import com.example.backend.service.member.AdminService; // 引用 AdminService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@CrossOrigin(origins = "http://localhost:5173")
public class MaintenanceController {

    @Autowired
    private MaintenanceStaffService staffService;

    @Autowired
    private MaintenanceInformationService mtifService;

    @Autowired
    private AdminService adminService; // ★ 確保這裡有注入

    @Autowired
    private AttachmentService attachmentService; // ★ 新增：附件服務

    @Autowired
    private HttpServletRequest request; // ★ 注入 HttpServletRequest 用於讀取 Session

    /**
     * ★ [問題 1 完整解決方案] 智慧取得當前登入管理員的真實姓名
     * 
     * 查找優先順序：
     * 1. 從 Session 中的 loginAdmin 取得 (最可靠)
     * 2. 從 SecurityContext 取得 username 並查詢
     * 3. 如果 username 是數字，當作 ID 查詢
     * 4. 如果都失敗，回傳預設值「系統管理員」
     */
    private String getCurrentUser() {
        System.out.println("========== [DEBUG] getCurrentUser() 開始 ==========");
        
        try {
            // ==========================================
            // 方法 1：從 Session 中直接取得 (最準確)
            // ==========================================
            HttpSession session = request.getSession(false);
            if (session != null) {
                Admin sessionAdmin = (Admin) session.getAttribute("loginAdmin");
                if (sessionAdmin != null && sessionAdmin.getAdmName() != null) {
                    String name = sessionAdmin.getAdmName();
                    System.out.println("[DEBUG] ✅ 從 Session 取得管理員: " + name);
                    System.out.println("[DEBUG] 管理員帳號: " + sessionAdmin.getAdmUsername());
                    System.out.println("========== [DEBUG] getCurrentUser() 結束 ==========");
                    return name;
                }
            }
            System.out.println("[DEBUG] ⚠️ Session 中找不到 loginAdmin");

            // ==========================================
            // 方法 2：從 SecurityContext 取得
            // ==========================================
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                System.out.println("[DEBUG] ⚠️ SecurityContext 沒有認證資訊");
                System.out.println("========== [DEBUG] getCurrentUser() 結束 ==========");
                return "系統管理員";
            }

            String username = auth.getName();
            System.out.println("[DEBUG] SecurityContext Auth Name: '" + username + "'");
            
            // ==========================================
            // 步驟 1：嘗試用 username 查詢
            // ==========================================
            Admin admin = adminService.findByUsername(username);
            if (admin != null && admin.getAdmName() != null) {
                System.out.println("[DEBUG] ✅ 透過 username 找到管理員: " + admin.getAdmName());
                System.out.println("========== [DEBUG] getCurrentUser() 結束 ==========");
                return admin.getAdmName();
            }
            System.out.println("[DEBUG] ⚠️ findByUsername('" + username + "') 沒有結果");

            // ==========================================
            // 步驟 2：檢查是否為數字 (ID)
            // ==========================================
            if (username.matches("\\d+")) {
                try {
                    Integer adminId = Integer.parseInt(username);
                    System.out.println("[DEBUG] username 是數字，嘗試用 ID 查詢: " + adminId);
                    admin = adminService.findById(adminId);
                    if (admin != null && admin.getAdmName() != null) {
                        System.out.println("[DEBUG] ✅ 透過 ID 找到管理員: " + admin.getAdmName());
                        System.out.println("========== [DEBUG] getCurrentUser() 結束 ==========");
                        return admin.getAdmName();
                    }
                    System.out.println("[DEBUG] ⚠️ findById(" + adminId + ") 沒有結果");
                } catch (NumberFormatException e) {
                    System.out.println("[DEBUG] ⚠️ 轉換 ID 失敗: " + e.getMessage());
                }
            }

            // ==========================================
            // 步驟 3：檢查是否為 Email (選用)
            // ==========================================
            if (username.contains("@")) {
                System.out.println("[DEBUG] username 看起來像 Email: " + username);
                // 如果 AdminService 有 findByEmail 方法可以在這裡呼叫
                // Admin admin = adminService.findByEmail(username);
            }

        } catch (Exception e) {
            System.err.println("[ERROR] getCurrentUser() 發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }

        // ==========================================
        // Fallback：所有方法都失敗
        // ==========================================
        System.out.println("[DEBUG] ❌ 所有查找方法都失敗，使用預設值");
        System.out.println("========== [DEBUG] getCurrentUser() 結束 ==========");
        return "系統管理員";
    }

    @GetMapping("/spots")
    public List<SpotOptionDto> getSpotOptions() { 
        return mtifService.getSpotOptions(); 
    }

    // ================== 維護人員 (Staff) ==================
    @GetMapping("/staff")
    public List<MaintenanceStaffResponseDto> getAllStaff() { 
        return staffService.getAllStaff(); 
    }

    // ★ 前端下拉選單應該呼叫這支 API，才不會選到停用的人
    @GetMapping("/staff/active")
    public List<MaintenanceStaffResponseDto> getActiveStaff() { 
        return staffService.getActiveStaff(); 
    }

    @GetMapping("/staff/{id}")
    public MaintenanceStaffResponseDto getStaffById(@PathVariable Integer id) { 
        return staffService.getStaffById(id); 
    }

    @PostMapping("/staff")
    public MaintenanceStaffResponseDto createStaff(@RequestBody MaintenanceStaff staff) { 
        return staffService.createStaff(staff); 
    }

    @PutMapping("/staff/{id}")
    public MaintenanceStaffResponseDto updateStaff(@PathVariable Integer id, @RequestBody MaintenanceStaff staff) { 
        return staffService.updateStaff(id, staff); 
    }

    @DeleteMapping("/staff/{id}")
    public void deleteStaff(@PathVariable Integer id) { 
        staffService.deleteStaff(id); 
    }

    @GetMapping("/staff/inactive")
    public List<MaintenanceStaff> getInactiveStaff() { 
        return staffService.getInactiveStaff(); 
    }

    // ================== 工單查詢 ==================
    @GetMapping("/tickets/active")
    public List<MaintenanceInformation> getActiveTickets() { 
        return mtifService.getActiveTickets(); 
    }

    @GetMapping("/tickets/history")
    public List<MaintenanceInformation> getHistoryTickets() { 
        return mtifService.getHistoryTickets(); 
    }

    @GetMapping("/tickets")
    public List<MaintenanceInformation> getAllTickets() { 
        return mtifService.getAllTickets(); 
    }

    @GetMapping("/tickets/spot/{spotId}")
    public List<MaintenanceInformation> getTicketsBySpot(@PathVariable Integer spotId) { 
        return mtifService.getTicketsBySpotId(spotId); 
    }

    @GetMapping("/tickets/{id}")
    public MaintenanceInformation getTicketById(@PathVariable Integer id) { 
        return mtifService.getRequiredTicket(id); 
    }

    // ================== 工單操作 (傳遞 operator) ==================

    @PostMapping("/tickets")
    public MaintenanceInformation createTicket(@RequestBody MaintenanceInformation mtif) {
        // ★ 這裡傳入 getCurrentUser() 確保 Log 紀錄正確名字
        return mtifService.createTicket(mtif, getCurrentUser());
    }

    @PutMapping("/tickets/{id}")
    public MaintenanceInformation updateTicket(@PathVariable Integer id, @RequestBody MaintenanceInformation mtif) {
        mtif.setTicketId(id);
        return mtifService.updateTicket(mtif, getCurrentUser());
    }
 
    @PostMapping("/tickets/{id}/assign")
    public void assignStaff(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        mtifService.assignStaff(id, body.get("staffId"), getCurrentUser());
    }

    @PostMapping("/tickets/{id}/start")
    public void startTicket(@PathVariable Integer id) {
        // 開始維修通常是維修人員自己按的
        mtifService.startTicket(id);
    }

    @PostMapping("/tickets/{id}/resolve")
    public void resolveTicket(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        mtifService.resolveTicket(id, body.get("resultType"), body.get("resolveNote"));
    }

    @PostMapping("/tickets/{id}/cancel")
    public void cancelTicket(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        mtifService.cancelTicket(id, body.get("reason"), getCurrentUser());
    }

    // ★ [問題 4 補充] 確保人員移轉與刪除時，也記錄操作者
    @PostMapping("/staff/transfer-and-delete")
    public void transferAndDelete(@RequestBody Map<String, Integer> body) {
        staffService.transferAndDelete(body.get("targetStaffId"), body.get("deleteStaffId"), getCurrentUser());
    }

    // ================== 其他 ==================
    @GetMapping("/seats")
    public List<Seat> getAllSeats() { return mtifService.getAllSeats(); }

    @GetMapping("/seats/spot/{spotId}")
    public List<Seat> getSeatsBySpot(@PathVariable Integer spotId) { return mtifService.getSeatsBySpot(spotId); }

    @GetMapping("/tickets/{id}/logs")
    public List<MaintenanceLogResponseDto> getTicketLogs(@PathVariable Integer id) { return mtifService.getTicketLogs(id); }
    
    @GetMapping("/staff/{staffId}/tickets")
    public List<MaintenanceInformation> getTicketsByStaff(@PathVariable Integer staffId, @RequestParam(required = false) List<String> statuses) {
        return mtifService.getTicketsByStaff(staffId, statuses);
    }

    // ================== 工單附件 API ==================

    /**
     * 上傳附件（多檔）
     * POST /api/maintenance/tickets/{ticketId}/attachments
     * 
     * @param ticketId 工單 ID
     * @param files 檔案陣列（必填）
     * @param note 備註（可選，套用到所有附件）
     * @return 附件清單
     */
    @PostMapping("/tickets/{ticketId}/attachments")
    public List<AttachmentResponseDto> uploadAttachments(
            @PathVariable Integer ticketId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "note", required = false) String note) {
        return attachmentService.uploadAttachments(ticketId, files, note);
    }

    /**
     * 查詢工單的所有附件
     * GET /api/maintenance/tickets/{ticketId}/attachments
     * 
     * @param ticketId 工單 ID
     * @return 附件清單（僅回傳啟用中的）
     */
    @GetMapping("/tickets/{ticketId}/attachments")
    public List<AttachmentResponseDto> getAttachments(@PathVariable Integer ticketId) {
        return attachmentService.getAttachments(ticketId);
    }

    /**
     * 刪除附件（軟刪除）
     * DELETE /api/maintenance/attachments/{attachmentId}
     * 
     * @param attachmentId 附件 ID
     * @return 刪除後的附件資訊
     */
    @DeleteMapping("/attachments/{attachmentId}")
    public AttachmentResponseDto deleteAttachment(@PathVariable Integer attachmentId) {
        return attachmentService.deleteAttachment(attachmentId);
    }
}