package com.example.backend.controller.member;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import com.example.backend.model.member.Member;
import com.example.backend.repository.member.MemberRepository;
import jakarta.servlet.http.HttpSession; // 記得加這個 import
import java.util.Optional;

@RestController
@RequestMapping("/member")
public class MemberProfileController {

    private final MemberRepository memberRepository;

    public MemberProfileController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * 取得會員資料 (相容一般登入與 Google 登入)
     */
    @GetMapping("/profile")
    public Object getProfile(Authentication authentication, HttpSession session) {

        // 1. 先找有沒有 Google 登入 (Spring Security)
        if (authentication != null && authentication.isAuthenticated()) {
            String email = "";
            if (authentication.getPrincipal() instanceof OAuth2User) {
                email = ((OAuth2User) authentication.getPrincipal()).getAttribute("email");
            } else {
                email = authentication.getName();
            }

            Optional<Member> memberOpt = memberRepository.findByMemEmail(email);
            if (memberOpt.isPresent()) {
                return memberOpt.get(); // 找到 Google 會員
            }
        }

        // 2. 如果上面沒找到，就找你原本一般登入存的 Session
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember != null) {
            // 為了怕資料過期，建議用 ID 再去資料庫撈一次最新的
            return memberRepository.findById(loginMember.getMemId()).orElse(loginMember);
        }

        return "尚未登入";
    }

    /**
     * 更新會員資料
     */
    @PutMapping("/profile")
    public Object updateProfile(@RequestBody Member formMember, Authentication authentication, HttpSession session) {
        Member targetMember = null;

        // 1. 識別身分 (優先看 Security，再看 Session)
        if (authentication != null && authentication.isAuthenticated()) {
            String email = (authentication.getPrincipal() instanceof OAuth2User)
                    ? ((OAuth2User) authentication.getPrincipal()).getAttribute("email")
                    : authentication.getName();
            targetMember = memberRepository.findByMemEmail(email).orElse(null);
        }

        if (targetMember == null) {
            targetMember = (Member) session.getAttribute("loginMember");
        }

        if (targetMember == null)
            return "尚未登入";

        // ===== 格式驗證 (保留你的邏輯) =====
        if (formMember.getMemEmail() == null || !formMember.getMemEmail().matches("^[A-Za-z0-9+_.-]+@.+\\.com$")) {
            return "Email 格式錯誤";
        }
        if (formMember.getMemPhone() == null || !formMember.getMemPhone().matches("^09\\d{8}$")) {
            return "手機格式錯誤";
        }
        if (formMember.getMemInvoice() != null && !formMember.getMemInvoice().matches("^/.{7}$")) {
            return "發票載具格式錯誤";
        }

        // ===== 更新資料 =====
        targetMember.setMemName(formMember.getMemName());
        targetMember.setMemPhone(formMember.getMemPhone());
        targetMember.setMemEmail(formMember.getMemEmail());
        targetMember.setMemInvoice(formMember.getMemInvoice());

        memberRepository.save(targetMember);

        // 2. 如果是一般會員，更新完後同步刷回 Session 確保下次讀取正確
        if (session.getAttribute("loginMember") != null) {
            session.setAttribute("loginMember", targetMember);
        }

        return "更新成功";
    }
}