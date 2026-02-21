package com.example.backend.controller.support;

import com.example.backend.dto.support.CozeBootstrapResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Support Coze Controller
 * 
 * 【重構說明】2026-02-01
 * - 移除 WebSDK/chatapp 整合（已確認 502 TLB 問題無法解決）
 * - 改用 Coze OpenAPI（POST /v3/chat）
 * - 後端作為 Proxy，前端不直接接觸 PAT
 * 
 * 端點：
 * - GET  /api/support/coze/bootstrap  → 配置資訊（保留相容）
 * - POST /api/support/coze/chat       → OpenAPI Proxy（新增）
 * - GET  /api/support/coze/status     → API 狀態檢查（新增）
 * 
 * 安全性：
 * 1. PAT 僅在後端使用，不傳給前端
 * 2. 不在 log 印出完整 token
 */
@RestController
@RequestMapping("/api/support/coze")
public class SupportCozeController {

    private static final Logger log = LoggerFactory.getLogger(SupportCozeController.class);
    
    // ==================== BEGIN: OpenAPI 常數 ====================
    private static final String COZE_API_BASE = "https://api.coze.com";
    private static final String COZE_CHAT_ENDPOINT = "/v3/chat";
    private static final int API_TIMEOUT_MS = 30000;
    private static final int MAX_RETRIES = 2;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    // ==================== END: OpenAPI 常數 ====================

    /**
     * Coze Bot ID
     * 設定方式：application.yml 的 coze.bot-id 或環境變數 COZE_BOT_ID
     */
    @Value("${coze.bot-id:}")
    private String botId;

    /**
     * Coze Personal Access Token (PAT)
     * 設定方式：application.yml 的 coze.pat 或環境變數 COZE_PAT
     * 
     * 注意：PAT 有以下特性
     * 1. 只能在建立時看到一次，無法再次查看
     * 2. 過期後無法延長，只能重新建立
     * 3. 建議定期更換（例如：每 90 天）
     */
    @Value("${coze.pat:}")
    private String pat;

    /**
     * Coze Web Chat SDK Script 來源 URL
     * 設定方式：application.yml 的 coze.chat-sdk-src 或環境變數 COZE_CHAT_SDK_SRC
     * 
     * 預設值：Coze 官方 CDN
     */
    @Value("${coze.chat-sdk-src:}")
    private String chatSdkSrc;

    /**
     * 平台提示（用於診斷）
     * 例如 "coze.com" 或 "coze.cn"
     */
    @Value("${coze.platform-hint:coze.com}")
    private String platformHint;

    /**
     * API 主機提示（用於前端診斷連通性）
     */
    @Value("${coze.api-host-hint:api.coze.com}")
    private String apiHostHint;

    // ==================== BEGIN: RestTemplate for OpenAPI ====================
    // 移除 RestTemplate，改用 HttpURLConnection 來處理 SSE
    
    public SupportCozeController() {
        // 不再需要 RestTemplate
    }
    
    /**
     * 遮罩 PAT 用於 log（只顯示前後各 4 字元）
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 12) return "***";
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
    // ==================== END: RestTemplate ====================

    // ==================== BEGIN: OpenAPI 狀態檢查 ====================
    /**
     * GET /api/support/coze/status
     * 
     * 檢查 Coze OpenAPI 配置是否完整（不執行實際 API 呼叫）
     * 用於前端判斷是否可開啟聊天
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkApiStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        
        // 驗證配置是否完整
        if (botId == null || botId.trim().isEmpty()) {
            result.put("status", "unconfigured");
            result.put("message", "Bot ID 未設定");
            result.put("available", false);
            return ResponseEntity.ok(result);
        }
        
        if (pat == null || pat.trim().isEmpty()) {
            result.put("status", "unconfigured");
            result.put("message", "PAT Token 未設定");
            result.put("available", false);
            return ResponseEntity.ok(result);
        }
        
        // 配置完整，標記為可用
        // 實際 API 可用性會在 /chat 端點呼叫時驗證
        result.put("status", "available");
        result.put("message", "OpenAPI 配置完整，可開始對話");
        result.put("available", true);
        result.put("mode", "openapi");
        result.put("botId", botId);
        
        log.info("✅ Coze OpenAPI 狀態檢查通過 - Bot ID: {}", botId);
        
        return ResponseEntity.ok(result);
    }
    // ==================== END: OpenAPI 狀態檢查 ====================

    // ==================== BEGIN: OpenAPI Chat Proxy (SSE Stream Mode) ====================
    /**
     * POST /api/support/coze/chat
     * 
     * Coze OpenAPI Proxy - 使用 stream=true 模式
     * 
     * 【重要】Coze V3 Chat API 在 stream=false 時只會回傳狀態（in_progress），
     * 必須使用 stream=true 並解析 SSE 事件流才能取得完整回覆。
     * 
     * SSE 事件類型：
     * - conversation.message.delta: 增量回覆片段
     * - conversation.message.completed: 訊息完成
     * - conversation.chat.completed: 對話完成
     * - conversation.chat.failed: 對話失敗
     * - error: 錯誤
     * 
     * Request Body:
     * {
     *   "message": "使用者訊息",
     *   "userId": "user_xxx"（可選）
     *   "conversationId": "conv_xxx"（可選）
     * }
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        result.put("requestId", requestId);
        
        // ===== Step 1: 驗證配置 =====
        if (botId == null || botId.trim().isEmpty()) {
            log.warn("[{}] ⚠️ Chat 失敗: Bot ID 未設定", requestId);
            result.put("success", false);
            result.put("error", "服務配置不完整");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }
        
        if (pat == null || pat.trim().isEmpty()) {
            log.warn("[{}] ⚠️ Chat 失敗: PAT 未設定", requestId);
            result.put("success", false);
            result.put("error", "服務配置不完整");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }
        
        // ===== Step 2: 取得使用者訊息 =====
        String userMessage = (String) request.get("message");
        if (userMessage == null || userMessage.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "訊息不可為空");
            return ResponseEntity.badRequest().body(result);
        }
        
        String userId = (String) request.getOrDefault("userId", "user_" + requestId);
        String conversationId = (String) request.get("conversationId");
        
        log.info("[{}] 📤 Chat 請求 - userId: {}, message 長度: {}, PAT: {}", 
                requestId, userId, userMessage.length(), maskToken(pat));
        
        // ===== Step 3: 呼叫 Coze OpenAPI (SSE 模式) =====
        int retryAttempt = 0;
        Exception lastException = null;
        
        while (retryAttempt <= MAX_RETRIES) {
            try {
                SseParseResult sseResult = callCozeApiWithSse(
                    requestId, userMessage, userId, conversationId, retryAttempt
                );
                
                if (sseResult.success) {
                    result.put("success", true);
                    result.put("replyText", sseResult.replyText);
                    result.put("conversationId", sseResult.conversationId);
                    result.put("chatId", sseResult.chatId);
                    
                    log.info("[{}] ✅ Chat 成功 - conversationId: {}, 回覆長度: {}", 
                            requestId, sseResult.conversationId, sseResult.replyText.length());
                    
                    return ResponseEntity.ok(result);
                } else {
                    // 業務錯誤（不重試）
                    if (sseResult.isBusinessError) {
                        result.put("success", false);
                        result.put("error", sseResult.errorMessage);
                        result.put("errorCode", sseResult.errorCode);
                        result.put("isBusinessError", true);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
                    }
                    
                    // 其他錯誤，可能需要重試
                    lastException = new RuntimeException(sseResult.errorMessage);
                }
                
            } catch (Exception e) {
                lastException = e;
                log.warn("[{}] ⚠️ 第 {} 次嘗試失敗: {}", requestId, retryAttempt + 1, e.getMessage());
            }
            
            retryAttempt++;
            if (retryAttempt <= MAX_RETRIES) {
                try {
                    Thread.sleep(500 * retryAttempt); // 指數退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // 所有重試都失敗
        log.error("[{}] ❌ Chat 失敗（已重試 {} 次）: {}", 
                requestId, MAX_RETRIES, lastException != null ? lastException.getMessage() : "Unknown");
        result.put("success", false);
        result.put("error", "連線失敗，請稍後再試");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
    }
    
    /**
     * SSE 解析結果
     */
    private static class SseParseResult {
        boolean success = false;
        String replyText = "";
        String conversationId;
        String chatId;
        String errorMessage;
        Integer errorCode;
        boolean isBusinessError = false;
    }
    
    /**
     * 呼叫 Coze API 並解析 SSE 事件流
     */
    private SseParseResult callCozeApiWithSse(
            String requestId, 
            String userMessage, 
            String userId, 
            String conversationId,
            int attempt) throws Exception {
        
        SseParseResult result = new SseParseResult();
        HttpURLConnection conn = null;
        
        try {
            // ===== 建立連線 =====
            URL url = new URL(COZE_API_BASE + COZE_CHAT_ENDPOINT);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(API_TIMEOUT_MS);
            conn.setReadTimeout(API_TIMEOUT_MS);
            conn.setDoOutput(true);
            
            // ===== 設定 Header =====
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + pat);
            conn.setRequestProperty("Accept", "text/event-stream"); // 關鍵：接受 SSE
            
            // ===== 建構請求 Body =====
            Map<String, Object> cozeRequest = new HashMap<>();
            cozeRequest.put("bot_id", botId);
            cozeRequest.put("user_id", userId);
            cozeRequest.put("stream", true);  // 關鍵：啟用串流模式
            cozeRequest.put("auto_save_history", true);
            
            // additional_messages
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            userMsg.put("content_type", "text");
            messages.add(userMsg);
            cozeRequest.put("additional_messages", messages);
            
            // 若有 conversationId，加入請求
            if (conversationId != null && !conversationId.isEmpty()) {
                cozeRequest.put("conversation_id", conversationId);
            }
            
            String requestBody = objectMapper.writeValueAsString(cozeRequest);
            
            log.info("[{}] 📡 呼叫 Coze OpenAPI (SSE, 第 {} 次): {}", 
                    requestId, attempt + 1, url);
            
            // ===== 發送請求 =====
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            log.info("[{}] 📥 HTTP 回應碼: {}", requestId, responseCode);
            
            // ===== 處理非 2xx 回應 =====
            if (responseCode != 200) {
                String errorBody = readErrorStream(conn);
                log.error("[{}] ❌ Coze API 錯誤 - HTTP {}: {}", requestId, responseCode, errorBody);
                
                // 嘗試解析錯誤訊息
                try {
                    JsonNode errorJson = objectMapper.readTree(errorBody);
                    int code = errorJson.path("code").asInt(0);
                    String msg = errorJson.path("msg").asText("API 錯誤");
                    
                    result.errorCode = code;
                    result.errorMessage = mapCozeErrorMessage(code, msg);
                    result.isBusinessError = (code >= 4000 && code < 5000);
                } catch (Exception e) {
                    result.errorMessage = "HTTP " + responseCode;
                }
                
                return result;
            }
            
            // ===== 解析 SSE 事件流 =====
            StringBuilder replyBuffer = new StringBuilder();
            String currentConversationId = null;
            String currentChatId = null;
            String logId = null;
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                
                String line;
                String currentEvent = null;
                StringBuilder dataBuffer = new StringBuilder();
                
                while ((line = reader.readLine()) != null) {
                    // SSE 格式: "event: xxx" 或 "data: xxx" 或空行
                    
                    if (line.startsWith("event:")) {
                        currentEvent = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        dataBuffer.append(line.substring(5).trim());
                    } else if (line.isEmpty() && dataBuffer.length() > 0) {
                        // 空行表示事件結束，處理事件
                        String eventData = dataBuffer.toString();
                        dataBuffer.setLength(0);
                        
                        if ("[DONE]".equals(eventData)) {
                            log.info("[{}] 📥 SSE 結束標記 [DONE]", requestId);
                            break;
                        }
                        
                        try {
                            JsonNode json = objectMapper.readTree(eventData);
                            
                            // 記錄 logid
                            if (json.has("logid")) {
                                logId = json.get("logid").asText();
                            }
                            
                            // 根據事件類型處理
                            if ("conversation.message.delta".equals(currentEvent)) {
                                // 增量回覆
                                String content = json.path("content").asText("");
                                if (!content.isEmpty()) {
                                    replyBuffer.append(content);
                                }
                            } else if ("conversation.chat.created".equals(currentEvent)) {
                                // 對話建立，取得 conversation_id
                                currentConversationId = json.path("conversation_id").asText();
                                currentChatId = json.path("id").asText();
                                log.info("[{}] 📥 對話建立 - conversationId: {}, chatId: {}", 
                                        requestId, currentConversationId, currentChatId);
                            } else if ("conversation.message.completed".equals(currentEvent)) {
                                // 訊息完成，可能包含完整回覆
                                String role = json.path("role").asText();
                                String type = json.path("type").asText();
                                if ("assistant".equals(role) && "answer".equals(type)) {
                                    String content = json.path("content").asText("");
                                    if (!content.isEmpty() && replyBuffer.length() == 0) {
                                        // 如果 delta 沒有累積到內容，用 completed 的完整內容
                                        replyBuffer.append(content);
                                    }
                                }
                            } else if ("conversation.chat.completed".equals(currentEvent)) {
                                // 對話完成
                                log.info("[{}] 📥 對話完成 - logId: {}", requestId, logId);
                            } else if ("conversation.chat.failed".equals(currentEvent) || 
                                       "error".equals(currentEvent)) {
                                // 錯誤
                                int code = json.path("code").asInt(0);
                                String msg = json.path("msg").asText(json.path("message").asText("錯誤"));
                                log.error("[{}] ❌ SSE 錯誤事件 - code: {}, msg: {}, logId: {}", 
                                        requestId, code, msg, logId);
                                
                                result.errorCode = code;
                                result.errorMessage = mapCozeErrorMessage(code, msg);
                                result.isBusinessError = (code >= 4000 && code < 5000);
                                return result;
                            }
                            
                        } catch (Exception e) {
                            log.warn("[{}] ⚠️ 解析 SSE data 失敗: {}", requestId, e.getMessage());
                        }
                        
                        currentEvent = null;
                    }
                }
            }
            
            // ===== 組裝結果 =====
            String finalReply = replyBuffer.toString().trim();
            
            if (finalReply.isEmpty()) {
                log.warn("[{}] ⚠️ SSE 解析完成但無回覆內容", requestId);
                result.errorMessage = "AI 未產生回覆";
                return result;
            }
            
            result.success = true;
            result.replyText = finalReply;
            result.conversationId = currentConversationId;
            result.chatId = currentChatId;
            
            log.info("[{}] ✅ SSE 解析成功 - 回覆長度: {}, 預覽: {}", 
                    requestId, finalReply.length(), 
                    finalReply.length() > 50 ? finalReply.substring(0, 50) + "..." : finalReply);
            
            return result;
            
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * 讀取錯誤回應
     */
    private String readErrorStream(HttpURLConnection conn) {
        try {
            java.io.InputStream errorStream = conn.getErrorStream();
            if (errorStream == null) return "";
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 將 Coze 錯誤碼轉換為使用者友好訊息
     */
    private String mapCozeErrorMessage(Integer code, String originalMsg) {
        if (code == null) return "未知錯誤";
        
        switch (code) {
            case 4100:
                return "授權失敗，請聯繫管理員";
            case 4101:
                return "Bot 未發布或無存取權限";
            case 4102:
                return "Bot ID 無效";
            case 4015:
                return "Bot 尚未發布到 API Channel，請聯繫管理員設定";
            case 4200:
                return "請求格式錯誤";
            default:
                return originalMsg != null ? originalMsg : "服務錯誤 (" + code + ")";
        }
    }
    // ==================== END: OpenAPI Chat Proxy ====================

    // ==================== BEGIN: Bootstrap（保留相容） ====================
    /**
     * GET /api/support/coze/bootstrap
     * 
     * 回應格式（已調整，移除 token）：
     * {
     *   "botId": "7469370888888888888",
     *   "mode": "openapi",
     *   "serverTime": "2026-01-31T12:00:00",
     *   "note": "已改用 OpenAPI 模式"
     * }
     * 
     * 注意：不再回傳 token，前端改用 /chat proxy
     * 
     * @return CozeBootstrapResponseDto
     */
    @GetMapping("/bootstrap")
    public ResponseEntity<?> getBootstrapConfig() {
        // ==================== Step 1：驗證必要配置（回傳 400 而非 500） ====================
        if (botId == null || botId.trim().isEmpty()) {
            log.warn("⚠️ Coze Bot ID 未設定");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Coze 配置不完整：缺少 Bot ID");
            errorResponse.put("hint", "請在 application.yml 設定 coze.bot-id 或環境變數 COZE_BOT_ID");
            errorResponse.put("status", 400);
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        if (pat == null || pat.trim().isEmpty()) {
            log.warn("⚠️ Coze PAT Token 未設定");
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Coze 配置不完整：缺少 PAT Token");
            errorResponse.put("hint", "請在 application.yml 設定 coze.pat 或環境變數 COZE_PAT");
            errorResponse.put("status", 400);
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        // ==================== Step 2：建立回應（OpenAPI 模式，不回傳 token） ====================
        Map<String, Object> response = new HashMap<>();
        response.put("botId", botId);
        response.put("mode", "openapi"); // 標示為 OpenAPI 模式
        response.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        response.put("note", "已改用 OpenAPI 模式，聊天請使用 POST /api/support/coze/chat");
        response.put("platformHint", platformHint);
        response.put("apiHostHint", apiHostHint);
        response.put("available", true);

        // ==================== Step 3：Log 輸出（不印 token） ====================
        log.info("✅ Coze Bootstrap 配置已提供 (OpenAPI 模式) - Bot ID: {}, Platform: {}", 
                botId, platformHint);

        return ResponseEntity.ok(response);
    }
    // ==================== END: Bootstrap ====================
}
