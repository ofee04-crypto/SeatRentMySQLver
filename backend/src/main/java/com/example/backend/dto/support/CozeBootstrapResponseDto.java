package com.example.backend.dto.support;

/**
 * Coze Web Chat SDK Bootstrap API 回應 DTO
 * 
 * 用途：提供前端初始化 Coze SDK 所需的配置資訊
 * 
 * 注意：
 * 1. token 欄位含敏感資訊，toString() 已做遮蔽處理
 * 2. expiresIn 若無法取得可設為 0 或 null
 * 3. 新增診斷欄位：platformHint, sdkMode, apiHostHint
 */
public class CozeBootstrapResponseDto {
    
    /**
     * Coze Bot ID（公開資訊）
     */
    private String botId;
    
    /**
     * 認證 Token（敏感資訊，需保密）
     */
    private String token;
    
    /**
     * Coze SDK Script 來源 URL
     */
    private String sdkSrc;
    
    /**
     * Token 過期時間（秒），若無法取得可為 null
     */
    private Integer expiresIn;
    
    /**
     * 伺服器時間（ISO-8601 格式）
     */
    private String serverTime;
    
    /**
     * 備註訊息（例如：提醒 PAT 過期時間）
     */
    private String note;

    // ==================== 新增：診斷欄位 ====================
    
    /**
     * 平台提示（例如 "coze.com" 或 "coze.cn"）
     * 用於診斷與區分不同區域的服務
     */
    private String platformHint;
    
    /**
     * 建議的 SDK 載入模式（"npm" | "script"）
     * 前端可根據此欄位決定載入策略
     */
    private String sdkMode;
    
    /**
     * API 主機提示（例如 "api.coze.com"）
     * 用於前端診斷連通性
     */
    private String apiHostHint;

    // Constructors
    public CozeBootstrapResponseDto() {}

    public CozeBootstrapResponseDto(String botId, String token, String sdkSrc) {
        this.botId = botId;
        this.token = token;
        this.sdkSrc = sdkSrc;
    }

    // Getters and Setters
    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = botId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSdkSrc() {
        return sdkSrc;
    }

    public void setSdkSrc(String sdkSrc) {
        this.sdkSrc = sdkSrc;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPlatformHint() {
        return platformHint;
    }

    public void setPlatformHint(String platformHint) {
        this.platformHint = platformHint;
    }

    public String getSdkMode() {
        return sdkMode;
    }

    public void setSdkMode(String sdkMode) {
        this.sdkMode = sdkMode;
    }

    public String getApiHostHint() {
        return apiHostHint;
    }

    public void setApiHostHint(String apiHostHint) {
        this.apiHostHint = apiHostHint;
    }

    /**
     * 重寫 toString()，避免印出完整 token
     * 僅顯示前 10 字元 + 遮蔽符號
     */
    @Override
    public String toString() {
        return "CozeBootstrapResponseDto{" +
                "botId='" + botId + '\'' +
                ", token='" + (token != null && token.length() > 10 
                        ? token.substring(0, 10) + "..." 
                        : "[HIDDEN]") + '\'' +
                ", sdkSrc='" + sdkSrc + '\'' +
                ", expiresIn=" + expiresIn +
                ", serverTime='" + serverTime + '\'' +
                ", platformHint='" + platformHint + '\'' +
                ", sdkMode='" + sdkMode + '\'' +
                ", apiHostHint='" + apiHostHint + '\'' +
                ", note='" + note + '\'' +
                '}';
    }
}
