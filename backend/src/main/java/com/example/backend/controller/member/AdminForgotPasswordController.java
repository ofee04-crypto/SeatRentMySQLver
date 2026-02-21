package com.example.backend.controller.member;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import com.example.backend.model.member.Admin;
import com.example.backend.service.member.AdminService;

@RestController
@RequestMapping("/api/admin/forgot-password")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminForgotPasswordController {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AdminService adminService;

    // 暫時用 Map 存管理員驗證碼 (Key: Email, Value: Code)
    private static Map<String, String> adminVerifyCodeStorage = new HashMap<>();

    /**
     * 發送驗證碼給管理員
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        // 1. 檢查資料庫是否有此 Email 的管理員
        Admin admin = adminService.findByEmail(email);
        if (admin == null) {
            return ResponseEntity.badRequest().body("該電子郵件未註冊為管理員");
        }

        // 2. 生成 6 位數驗證碼
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 3. 存入記憶體
        adminVerifyCodeStorage.put(email, code);

        // 4. 寄出郵件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("alan123149@gmail.com");
            message.setTo(email);
            message.setSubject("SeatRentSys - 管理員密碼重設驗證碼");
            message.setText("管理員您好，您的驗證碼為：" + code + "，請於 5 分鐘內輸入。若非本人操作請忽略此信。");
            mailSender.send(message);

            System.out.println("[管理員備份] 驗證碼已寄至: " + email + " 碼為: " + code);
            return ResponseEntity.ok("驗證碼已寄出");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("郵件發送失敗：" + e.getMessage());
        }
    }

    /**
     * 管理員重設密碼 (明文版)
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        // 1. 比對驗證碼
        String savedCode = adminVerifyCodeStorage.get(email);
        if (savedCode == null || !savedCode.equals(code)) {
            return ResponseEntity.badRequest().body("驗證碼錯誤或已過期");
        }

        // 2. 更新資料庫密碼
        try {
            // 直接呼叫 Service 更新明文密碼
            boolean success = adminService.updatePasswordByEmail(email, newPassword);

            if (success) {
                // 成功後移除驗證碼
                adminVerifyCodeStorage.remove(email);
                return ResponseEntity.ok("管理員密碼重設成功");
            } else {
                return ResponseEntity.status(500).body("資料庫更新失敗");
            }
        } catch (IllegalArgumentException e) {
            // 這裡會抓到 AdminService.validatePassword 丟出的格式錯誤
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("發生錯誤：" + e.getMessage());
        }
    }
}