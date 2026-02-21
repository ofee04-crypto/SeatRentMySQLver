package com.example.backend.file;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 通用上傳
     * - 預設存到 uploads/spots
     * - 回傳 filePath = "spots/uuid_xxx.jpg" (直接可存 DB)
     * - 回傳 url = "/images/spots/uuid_xxx.jpg" (前端可預覽)
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "spots") String folder) {
        // 1) 儲存檔案並取得「相對路徑」：例如 "spots/uuid_xxx.jpg"
        String filePath = fileStorageService.store(file, folder);

        // 2) 組出前端可直接用的 URL（對應 WebConfig: /images/** → uploads/）
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/images/")
                .path(filePath) // filePath 已含 "spots/..."
                .toUriString();

        // 3) 回傳 JSON（filePath 用於存 DB；url 用於預覽）
        return ResponseEntity.ok(Map.of(
                "filePath", filePath,
                "url", url));
    }
}
