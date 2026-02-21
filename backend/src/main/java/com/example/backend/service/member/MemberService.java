package com.example.backend.service.member;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.model.member.Member;
import com.example.backend.repository.member.MemberRepository;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // 密碼驗證
    private void validatePassword(String password) {
        String pwdRule = "^(?=.*[A-Za-z])[A-Za-z\\d!@#$%^&*()_+=\\[\\]{}:;\"'<>,.?/\\-]{6,}$";

        if (password == null || !password.matches(pwdRule)) {
            throw new IllegalArgumentException(
                    "密碼格式錯誤：至少6碼，需包含至少1個英文字母");
        }
    }

    // 查全部
    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    // 查單筆
    public Member findById(Integer memId) {
        return memberRepository.findById(memId).orElse(null);
    }

    // 新增（InsertMember）
    public void insert(Member member) {
        validatePassword(member.getMemPassword());
        member.setMemStatus(1);
        try {
            memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {

            throw new IllegalArgumentException("帳號或 Email 已被使用");
        }
    }

    // 修改（UpdateMember）
    public void update(Member old, String newPassword) {
        // 如果有新密碼，就驗證並設定
        if (newPassword != null && !newPassword.isBlank()) {
            validatePassword(newPassword);
            old.setMemPassword(newPassword);
        }

        // 不管有沒有改密碼，最後都要 save 剩下的欄位（如姓名、電話）
        memberRepository.save(old);
    }

    // 刪除（DeleteMember）
    public void deleteById(Integer memId) {
        memberRepository.deleteById(memId);
    }

    // 模糊查詢
    public List<Member> search(String keyword) {
        return memberRepository.findByKeyword(keyword);
    }

    // 透過 Email 尋找會員 (忘記密碼用)
    public Member findByEmail(String email) {
        // 這裡調用 repository 根據 Email 找人，請確保 repository 有對應的方法
        return memberRepository.findByMemEmailIgnoreCase(email);
    }

    // 透過 Email 更新密碼 (忘記密碼用)
    @Transactional
    public boolean updatePasswordByEmail(String email, String newPassword) {
        // 1. 先檢查密碼格式
        validatePassword(newPassword);

        // 2. 透過 email 尋找會員 (加上 .orElse(null) 解決型別不匹配)
        Member member = memberRepository.findByMemEmail(email.trim().toLowerCase()).orElse(null);

        if (member != null) {
            // 3. 設定新密碼並存檔
            member.setMemPassword(newPassword);
            memberRepository.save(member);
            return true;
        }
        return false;
    }
}