package com.example.backend.service.member;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.model.member.Admin;
import com.example.backend.repository.member.AdminRepository;

@Service
@Transactional
public class AdminService {

    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    // 密碼驗證
    private void validatePassword(String password) {
        String pwdRule = "^(?=.*[A-Za-z])[A-Za-z\\d!@#$%^&*()_+=\\[\\]{}:;\"'<>,.?/\\-]{6,}$";

        if (password == null || !password.matches(pwdRule)) {
            throw new IllegalArgumentException(
                    "密碼格式錯誤：至少6碼，需包含至少1個英文字母");
        }
    }

    private void normalize(Admin admin) {
        admin.setAdmUsername(admin.getAdmUsername().trim());
        admin.setAdmEmail(admin.getAdmEmail().trim().toLowerCase());
    }

    // 查全部（findAllAdmins）
    public List<Admin> findAll() {
        return adminRepository.findAll();
    }

    // 查單筆（findAdminById）
    public Admin findById(Integer admId) {
        return adminRepository.findById(admId).orElse(null);
    }

    // 登入用（findByUsername）
    public Admin findByUsername(String admUsername) {
        return adminRepository.findByAdmUsername(admUsername);
    }

    // 新增管理員（saveAdmin）
    public void insert(Admin admin) {
        validatePassword(admin.getAdmPassword());
        normalize(admin);

        if (adminRepository.existsByAdmUsername(admin.getAdmUsername())) {
            throw new IllegalArgumentException("管理員帳號已存在");
        }

        if (adminRepository.existsByAdmEmail(admin.getAdmEmail())) {
            throw new IllegalArgumentException("Email 已存在");
        }

        adminRepository.save(admin);
    }

    // 更新管理員（updateAdmin）
    public void update(Admin admin) {
        normalize(admin);
        adminRepository.save(admin);
    }

    // 刪除管理員（deleteAdmin）
    public void deleteById(Integer admId) {
        adminRepository.deleteById(admId);
    }

    // 模糊查詢
    public List<Admin> findByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return adminRepository.findAll();
        }
        return adminRepository.findByKeyword(keyword);
    }

    // 透過 Email 尋找管理員 (供 Controller 檢查是否存在)
    public Admin findByEmail(String email) {
        if (email == null)
            return null;
        return adminRepository.findByAdmEmail(email.trim().toLowerCase());
    }

    // 透過 Email 更新密碼
    @Transactional
    public boolean updatePasswordByEmail(String email, String newPassword) {
        // 先檢查密碼格式
        validatePassword(newPassword);

        Admin admin = adminRepository.findByAdmEmail(email.trim().toLowerCase());
        if (admin != null) {
            admin.setAdmPassword(newPassword);
            adminRepository.save(admin);
            return true;
        }
        return false;
    }
}