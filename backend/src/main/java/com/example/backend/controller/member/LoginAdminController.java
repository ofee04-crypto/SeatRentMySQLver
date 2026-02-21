package com.example.backend.controller.member;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder; // 新增
import org.springframework.web.bind.annotation.*;

import com.example.backend.model.member.Admin;
import com.example.backend.repository.member.AdminRepository;

import jakarta.servlet.http.HttpServletRequest; // 改用 HttpServletRequest 比較好操作 Session
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/login")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class LoginAdminController {

    private final AdminRepository adminRepository;

    public LoginAdminController(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @PostMapping("/admin")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
            HttpServletRequest request) { // 改傳入 request

        String username = body.get("admUsername");
        String password = body.get("admPassword");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("請輸入帳號與密碼");
        }

        Admin admin = adminRepository.findByAdmUsername(username);

        if (admin == null || !password.equals(admin.getAdmPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("帳號或密碼錯誤");
        }

        if (admin.getAdmStatus() != null && admin.getAdmStatus() == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("帳號已停權，請聯絡管理員");
        }

        // ==========================================
        // 核心修正：清理舊身分，防止 Gmail 身分污染
        // ==========================================

        // 1. 清除 Spring Security 的驗證資訊 (徹底踢掉 Gmail 身分)
        SecurityContextHolder.clearContext();

        // 2. 銷毀舊 Session 並建立新 Session (防止 Session Fixation 攻擊也清理舊資料)
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession newSession = request.getSession(true);

        // 3. 設定管理員登入資訊
        newSession.setAttribute("loginAdmin", admin);

        // ==========================================

        Map<String, Object> result = new HashMap<>();
        result.put("admUsername", admin.getAdmUsername());
        result.put("admName", admin.getAdmName());
        result.put("admRole", admin.getAdmRole());
        result.put("admId", admin.getAdmId()); // 補上 ID
        result.put("admEmail", admin.getAdmEmail()); // 補上 Email
        result.put("admCreatedAt", admin.getCreatedAt()); // 補上到職日期

        return ResponseEntity.ok(result);
    }
}
