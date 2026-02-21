package com.example.backend.repository.member;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.backend.model.member.Admin;

public interface AdminRepository extends JpaRepository<Admin, Integer> {

    // 登入
    Admin findByAdmUsername(String admUsername);

    // 新增前檢查用
    boolean existsByAdmUsername(String admUsername);

    boolean existsByAdmEmail(String admEmail);

    // 模糊查詢
    @Query("""
                SELECT a FROM Admin a
                WHERE a.admUsername LIKE %:kw%
                   OR a.admName LIKE %:kw%
                   OR a.admEmail LIKE %:kw%
            """)
    List<Admin> findByKeyword(@Param("kw") String keyword);

    // 忘記密碼用：透過 Email 找到管理員物件
    Admin findByAdmEmail(String admEmail);
}