package com.example.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全域例外處理器
 * 統一攔截 Controller 拋出的例外，並回傳標準 JSON 格式 { message: "..." }
 * 讓前端可以直接讀取 error.response.data.message 顯示錯誤訊息
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 處理參數驗證錯誤（業務邏輯驗證失敗）
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("status", 400);
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 處理狀態衝突錯誤（例如：狀態不允許執行某操作）
     * HTTP 409 Conflict
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("status", 409);
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * 處理靜態資源找不到錯誤（Spring Boot 3.x 新增）
     * HTTP 404 Not Found
     * 
     * 常見情境：
     * - 前端請求不存在的 API endpoint
     * - 請求不存在的靜態資源（圖片、CSS、JS 等）
     * 
     * 注意：此錯誤不應被視為系統錯誤（500），而是正常的 404 回應
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.warn("NoResourceFoundException: {} - {}", ex.getResourcePath(), ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", "Not Found");
        errorResponse.put("path", ex.getResourcePath());
        errorResponse.put("status", 404);
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * 處理執行階段錯誤（例如：檔案格式不對、系統存取問題）
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.warn("RuntimeException: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("status", 400);
        errorResponse.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * 處理所有未預期的例外
     * HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // 記錄完整 stack trace 到後端 log（不回傳給前端）
        log.error("Unexpected exception occurred", ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", "系統發生非預期錯誤，請稍後再試或聯繫管理員");
        errorResponse.put("status", 500);
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        // 開發環境可以加上 ex.getClass().getSimpleName() 方便除錯
        // errorResponse.put("type", ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
