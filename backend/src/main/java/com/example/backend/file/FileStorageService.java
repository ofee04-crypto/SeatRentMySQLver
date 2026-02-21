package com.example.backend.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;

@Service
public class FileStorageService {

    @Value("${app.file.upload-path:uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("無法初始化上傳目錄", e);
        }
    }

    public String store(MultipartFile file, String subdir) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("無法儲存空檔案");
        }
        if (subdir == null || subdir.isBlank()) {
            throw new RuntimeException("子資料夾(subdir)不得為空");
        }

        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("不支援的檔案格式！僅允許上傳圖片檔案 (MIME type: " + contentType + ")");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                throw new RuntimeException("檔案名稱不得為空");
            }

            String cleanFileName = java.nio.file.Paths.get(originalFilename).getFileName().toString();

            // 確保 uploads/subdir 存在
            java.nio.file.Path subdirPath = this.rootLocation.resolve(subdir).normalize();
            java.nio.file.Files.createDirectories(subdirPath);

            String filename = java.util.UUID.randomUUID() + "_" + cleanFileName;

            java.nio.file.Path destinationFile = subdirPath.resolve(filename).normalize().toAbsolutePath();

            // 安全性檢查：確保仍在 uploads(rootLocation) 下
            if (!destinationFile.startsWith(this.rootLocation.toAbsolutePath())) {
                throw new RuntimeException("無法將檔案儲存至預期目錄之外");
            }

            try (java.io.InputStream inputStream = file.getInputStream()) {
                java.nio.file.Files.copy(inputStream, destinationFile,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // ✅ A 模式：回傳 DB 要存的相對路徑
            return subdir + "/" + filename;

        } catch (java.io.IOException e) {
            throw new RuntimeException("檔案儲存失敗", e);
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank())
            return;

        try {
            java.nio.file.Path target = rootLocation.resolve(relativePath).normalize().toAbsolutePath();

            if (!target.startsWith(rootLocation.toAbsolutePath())) {
                throw new RuntimeException("拒絕刪除 uploads 目錄以外的檔案: " + relativePath);
            }

            java.nio.file.Files.deleteIfExists(target);

        } catch (java.io.IOException e) {
            throw new RuntimeException("無法刪除檔案: " + relativePath, e);
        }
    }
}