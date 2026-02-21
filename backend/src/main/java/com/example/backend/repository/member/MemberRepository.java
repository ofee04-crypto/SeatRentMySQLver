package com.example.backend.repository.member;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.model.member.Member;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    // 登入用（新增這行）
    Member findByMemUsername(String memUsername);

    // 模糊查詢
    @Query("""
                SELECT m FROM Member m
                WHERE m.memUsername LIKE %:kw%
                   OR m.memName LIKE %:kw%
                   OR m.memEmail LIKE %:kw%
                   OR m.memPhone LIKE %:kw%
            """)
    List<Member> findByKeyword(@Param("kw") String keyword);

    // 讓 CustomOAuth2UserService 可以用 Email 檢查會員是否存在
    Optional<Member> findByMemEmail(String memEmail);

    // --- 新增這行給忘記密碼功能使用 ---
    // 直接回傳 Member 物件，方便 Service 調用
    Member findByMemEmailIgnoreCase(String memEmail);
}