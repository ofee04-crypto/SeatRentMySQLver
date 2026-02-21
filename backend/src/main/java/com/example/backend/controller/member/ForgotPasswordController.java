package com.example.backend.controller.member;

import java.util.Map;
import java.util.Random;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.example.backend.model.member.Member;
import com.example.backend.service.member.MemberService;

@RestController
@RequestMapping("/api/forgot-password")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ForgotPasswordController {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MemberService memberService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // 暫時用一個 Map 存驗證碼（如果還沒接 Redis 的話）
    // Key 是 Email, Value 是 驗證碼
    private static Map<String, String> verifyCodeStorage = new HashMap<>();

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        // 1. 檢查資料庫是否有此 Email 的會員
        Member member = memberService.findByEmail(email); // 假設你 Service 有這方法
        if (member == null) {
            return ResponseEntity.badRequest().body("該電子郵件未註冊為會員");
        }

        // 2. 生成 6 位數驗證碼
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 3. 存入記憶體 (正式環境建議用 Redis 設定 5 分鐘過期)
        verifyCodeStorage.put(email, code);

        // 4. 寄出郵件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("alan123149@gmail.com");
            message.setTo(email);
            message.setSubject("SeatRentSys - 密碼重設驗證碼");
            message.setText("您好，您的驗證碼為：" + code + "，請於 5 分鐘內輸入。若非本人操作請忽略此信。");
            mailSender.send(message);

            System.out.println("驗證碼已寄至: " + email + " 碼為: " + code);
            return ResponseEntity.ok("驗證碼已寄出");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("郵件發送失敗：" + e.getMessage());
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        // 1. 比對驗證碼
        String savedCode = verifyCodeStorage.get(email);
        if (savedCode == null || !savedCode.equals(code)) {
            return ResponseEntity.badRequest().body("驗證碼錯誤或已過期");
        }

        // 2. 更新資料庫密碼 (改為直接存字串)
        try {
            // 直接傳入明文 newPassword，不要用 passwordEncoder 加密
            boolean success = memberService.updatePasswordByEmail(email, newPassword);

            if (success) {
                verifyCodeStorage.remove(email);
                return ResponseEntity.ok("密碼重設成功");
            } else {
                return ResponseEntity.status(500).body("資料庫更新失敗");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("發生錯誤：" + e.getMessage());
        }
    }
}