package com.example.backend.controller.member;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.model.member.Member;
import com.example.backend.repository.member.MemberRepository;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private MemberRepository memberRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        // 如果沒登入，principal 會是 null
        if (principal == null) {
            return ResponseEntity.status(401).body("未登入");
        }

        // 從 Google 資料中拿 Email
        String email = principal.getAttribute("email");

        // 去資料庫找該會員資料，回傳給前端
        Optional<Member> member = memberRepository.findByMemEmail(email);

        return member.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }
}
