package com.example.backend.service.maintenance;

import com.example.backend.dto.maintenance.AttachmentResponseDto;
import com.example.backend.model.maintenance.MaintenanceInformation;
import com.example.backend.model.maintenance.MaintenanceTicketAttachment;
import com.example.backend.repository.maintenance.MaintenanceInformationRepository;
import com.example.backend.repository.maintenance.MaintenanceTicketAttachmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 附件管理 Service
 */
@Service
@Transactional
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    // 允許的圖片格式
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );

    // 單檔最大 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 每次最多 5 張
    private static final int MAX_FILES_PER_REQUEST = 5;

    @Value("${app.file.upload-path:uploads}")
    private String uploadBasePath;

    private final MaintenanceTicketAttachmentRepository attachmentRepo;
    private final MaintenanceInformationRepository ticketRepo;

    public AttachmentService(
            MaintenanceTicketAttachmentRepository attachmentRepo,
            MaintenanceInformationRepository ticketRepo) {
        this.attachmentRepo = attachmentRepo;
        this.ticketRepo = ticketRepo;
    }

    /**
     * 上傳附件（多檔）
     * 
     * @param ticketId 工單 ID
     * @param files 檔案陣列
     * @param note 備註（可選，套用到所有附件）
     * @return 附件 DTO 清單
     */
    public List<AttachmentResponseDto> uploadAttachments(Integer ticketId, MultipartFile[] files, String note) {
        // 1. 驗證工單是否存在
        MaintenanceInformation ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("找不到工單 ID: " + ticketId));

        // 2. 驗證檔案數量
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("請選擇至少一個檔案");
        }
        if (files.length > MAX_FILES_PER_REQUEST) {
            throw new IllegalArgumentException("一次最多上傳 " + MAX_FILES_PER_REQUEST + " 個檔案");
        }

        // 3. 取得當前最大 sortOrder
        Integer maxSortOrder = attachmentRepo.findMaxSortOrderByTicketId(ticketId);
        int nextSortOrder = (maxSortOrder != null ? maxSortOrder : 0) + 1;

        // 4. 建立儲存目錄
        Path ticketDir = Paths.get(uploadBasePath, "maintenance", "tickets", ticketId.toString())
                .toAbsolutePath().normalize();
        try {
            if (!Files.exists(ticketDir)) {
                Files.createDirectories(ticketDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("無法建立儲存目錄: " + e.getMessage(), e);
        }

        // 5. 逐一處理檔案
        List<AttachmentResponseDto> results = new ArrayList<>();
        for (MultipartFile file : files) {
            // 5.1 驗證檔案
            validateFile(file);

            // 5.2 生成唯一檔名
            String storedName = generateUniqueStoredName(ticketId, file.getOriginalFilename());

            // 5.3 儲存檔案
            Path targetPath = ticketDir.resolve(storedName);
            try {
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("檔案儲存失敗: " + e.getMessage(), e);
            }

            // 5.4 建立 Entity
            MaintenanceTicketAttachment attachment = new MaintenanceTicketAttachment();
            attachment.setTicket(ticket);
            attachment.setTicketId(ticketId);
            attachment.setOriginalName(sanitizeFileName(file.getOriginalFilename()));
            attachment.setStoredName(storedName);
            attachment.setContentType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setPublicUrl(String.format("/images/maintenance/tickets/%d/%s", ticketId, storedName));
            attachment.setSortOrder(nextSortOrder++);
            attachment.setNote(note);
            attachment.setIsActive(true);

            // 5.5 儲存到 DB
            MaintenanceTicketAttachment saved = attachmentRepo.save(attachment);
            results.add(new AttachmentResponseDto(saved));

            log.info(" 附件上傳成功: ticketId={}, attachmentId={}, storedName={}", 
                    ticketId, saved.getAttachmentId(), storedName);
        }

        return results;
    }

    /**
     * 查詢工單的所有啟用附件
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponseDto> getAttachments(Integer ticketId) {
        // 驗證工單是否存在
        if (!ticketRepo.existsById(ticketId)) {
            throw new IllegalArgumentException("找不到工單 ID: " + ticketId);
        }

        List<MaintenanceTicketAttachment> attachments = attachmentRepo
                .findByTicketIdAndIsActiveTrueOrderBySortOrderAscCreatedAtDesc(ticketId);

        return attachments.stream()
                .map(AttachmentResponseDto::new)
                .toList();
    }

    /**
     * 刪除附件（軟刪除）
     */
    public AttachmentResponseDto deleteAttachment(Integer attachmentId) {
        // 1. 查詢附件
        MaintenanceTicketAttachment attachment = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("找不到附件 ID: " + attachmentId));

        // 2. 軟刪除（設定 isActive = false）
        attachment.setIsActive(false);
        MaintenanceTicketAttachment updated = attachmentRepo.save(attachment);

        // 3. 嘗試刪除實體檔案（失敗不影響 API 回應）
        try {
            Path filePath = Paths.get(uploadBasePath, "maintenance", "tickets", 
                    attachment.getTicketId().toString(), attachment.getStoredName())
                    .toAbsolutePath().normalize();

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("✅ 實體檔案已刪除: {}", filePath);
            } else {
                log.warn("⚠️ 實體檔案不存在: {}", filePath);
            }
        } catch (Exception e) {
            log.error("❌ 刪除實體檔案失敗（不影響 API 回應）: {}", e.getMessage());
        }

        log.info("✅ 附件已軟刪除: attachmentId={}, ticketId={}", attachmentId, attachment.getTicketId());
        return new AttachmentResponseDto(updated);
    }

    // ==================== 私有方法 ====================

    /**
     * 驗證檔案
     */
    private void validateFile(MultipartFile file) {
        // 驗證是否為空
        if (file.isEmpty()) {
            throw new IllegalArgumentException("檔案不可為空");
        }

        // 驗證檔案類型
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("不支援的檔案格式: %s（僅允許 %s）", contentType, String.join(", ", ALLOWED_CONTENT_TYPES))
            );
        }

        // 驗證檔案大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("檔案大小超過限制（最大 %.2f MB）", MAX_FILE_SIZE / 1024.0 / 1024.0)
            );
        }
    }

    /**
     * 生成唯一的儲存檔名（UUID + 原副檔名）
     * 防止重複，確保滿足 UX_mta_ticket_storedName 唯一性約束
     */
    private String generateUniqueStoredName(Integer ticketId, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        String storedName;
        int attempts = 0;
        do {
            storedName = UUID.randomUUID().toString() + extension;
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("無法生成唯一檔名（嘗試次數過多）");
            }
        } while (attachmentRepo.existsByTicketIdAndStoredName(ticketId, storedName));

        return storedName;
    }

    /**
     * 淨化檔名（防止路徑穿越攻擊）
     */
    private String sanitizeFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unknown";
        }
        // 只取檔案名稱（移除路徑）
        return Paths.get(filename).getFileName().toString();
    }
}
