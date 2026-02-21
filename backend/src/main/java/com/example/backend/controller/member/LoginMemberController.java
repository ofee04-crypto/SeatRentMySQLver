package com.example.backend.controller.member;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.model.member.Member;
import com.example.backend.repository.member.MemberRepository;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/login")
@CrossOrigin(origins = "http://localhost:5173")
public class LoginMemberController {

    @Autowired
    private MemberRepository memberRepository;

    @PostMapping("/member")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
            HttpSession session) {

        String username = body.get("memUsername");
        String password = body.get("memPassword");

        // 基本防呆
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body("請輸入帳號與密碼");
        }

        // 查詢會員
        Member member = memberRepository.findByMemUsername(username);

        // 帳號不存在
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("帳號或密碼錯誤");
        }

        // 帳號停權
        if (member.getMemStatus() == 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("您的會員帳號已停權，請聯絡客服人員");
        }

        // =========================修正登不進去的問題=========================
        // 我們先加上 .trim() 來去除前後空白字元，造成燈不進去的問題
        if (!password.trim().equals(member.getMemPassword().trim())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("帳號或密碼錯誤");
        }
        // ========================分隔線 =========================================

        // 密碼比對
        if (!password.equals(member.getMemPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("帳號或密碼錯誤");
        }

        // 登入成功，存 session
        session.setAttribute("loginMember", member);

        // 回成功訊息
        return ResponseEntity.ok(member);
    }
}
