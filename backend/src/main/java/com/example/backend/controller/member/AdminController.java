package com.example.backend.controller.member;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.model.member.Admin;
import com.example.backend.service.member.AdminService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/admins")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // 查全部
    @GetMapping
    public List<Admin> findAll() {
        return adminService.findAll();
    }

    // 查單筆
    @GetMapping("/find")
    public Object findOne(@RequestParam(required = false) Integer admId) {

        if (admId == null) {
            return "請輸入 admId";
        }

        Admin admin = adminService.findById(admId);
        return admin != null ? admin : "查無此管理員";
    }

    // 新增
    @PostMapping
    public ResponseEntity<?> insert(@RequestBody Admin admin) {
        try {
            adminService.insert(admin);
            return ResponseEntity.ok("管理員新增成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 修改
    @PostMapping("/update")
    public String update(@RequestBody Admin admin) {

        Admin old = adminService.findById(admin.getAdmId());
        if (old == null) {
            return "查無此管理員";
        }

        old.setAdmUsername(admin.getAdmUsername());
        old.setAdmName(admin.getAdmName());
        old.setAdmEmail(admin.getAdmEmail());
        old.setAdmRole(admin.getAdmRole());
        old.setUpdatedAt(java.time.LocalDateTime.now());
        //old.setAdminImage(admin.getAdminImage());
        // 密碼：有傳才更新
        if (admin.getAdmPassword() != null && !admin.getAdmPassword().isBlank()) {
            old.setAdmPassword(admin.getAdmPassword());
        }

        adminService.update(old);
        return "管理員修改成功";
    }

    // 刪除
    @GetMapping("/delete")
    public String delete(@RequestParam Integer admId) {
        adminService.deleteById(admId);
        return "管理員刪除成功（admId=" + admId + "）";
    }

    // 模糊查詢
    @GetMapping("/search")
    public List<Admin> search(@RequestParam String keyword) {
        return adminService.findByKeyword(keyword);
    }

    // 停權
    @GetMapping("/disable")
    public String disable(@RequestParam Integer admId) {

        Admin admin = adminService.findById(admId);
        if (admin == null) {
            return "查無此管理員";
        }

        admin.setAdmStatus(0);
        adminService.update(admin);

        return "管理員已停權（admId=" + admId + "）";
    }

    // 啟用
    @GetMapping("/enable")
    public String enable(@RequestParam Integer admId) {

        Admin admin = adminService.findById(admId);
        if (admin == null) {
            return "查無此管理員";
        }

        admin.setAdmStatus(1);
        adminService.update(admin);

        return "管理員已啟用（admId=" + admId + "）";
    }

    // 停職 API (對應前端的 confirmBanAdmin)
    @PostMapping("/ban")
    public ResponseEntity<?> banAdmin(@RequestBody Map<String, Object> payload) {
        try {
            Object idObj = payload.get("admId");
            if (idObj == null)
                return ResponseEntity.badRequest().body("缺少 admId");
            Integer admId = Integer.valueOf(idObj.toString());

            Admin admin = adminService.findById(admId);
            if (admin == null)
                return ResponseEntity.status(404).body("找不到該管理員");

            // 修改狀態
            admin.setAdmStatus(0);
            admin.setUpdatedAt(java.time.LocalDateTime.now());

            // 關鍵：因為 Service 的 update 會跑 normalize (檢查 trim)
            // 如果你的資料庫欄位有空值，這邊建議直接用 repository 存，
            // 或者確保 admin 物件內容是完整的。
            // 這裡我們直接呼叫原本的 update：
            adminService.update(admin);

            return ResponseEntity.ok("管理員已停職");
        } catch (Exception e) {
            e.printStackTrace(); // 這行很重要，請看 Eclipse/IntelliJ 的 Console 報什麼錯
            return ResponseEntity.status(500).body("操作失敗：" + e.getMessage());
        }
    }

    // 重新啟用 API (對應前端的 activateAdmin)
    @PostMapping("/activate")
    public ResponseEntity<?> activateAdmin(@RequestBody Map<String, Object> payload) {
        try {
            // 取得 admId
            Integer admId = (Integer) payload.get("admId");

            Admin admin = adminService.findById(admId);
            if (admin == null) {
                return ResponseEntity.status(404).body("找不到該管理員");
            }

            // 將狀態設為 1 (啟用)
            admin.setAdmStatus(1);
            admin.setUpdatedAt(java.time.LocalDateTime.now());

            // 儲存變更
            adminService.update(admin);

            return ResponseEntity.ok("管理員已重新啟用");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("啟用失敗：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Integer id, @RequestBody Map<String, Integer> payload) {
        try {
            // 透過 Service 找到管理員
            Admin admin = adminService.findById(id);
            if (admin == null) {
                return ResponseEntity.status(404).body("找不到該管理員");
            }

            // 更新權限
            admin.setAdmRole(payload.get("admRole"));
            admin.setUpdatedAt(java.time.LocalDateTime.now()); // 確保更新時間有變動

            // 呼叫 Service 的 update 方法存回資料庫
            adminService.update(admin);

            return ResponseEntity.ok("權限更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("更新失敗：" + e.getMessage());
        }
    }
}