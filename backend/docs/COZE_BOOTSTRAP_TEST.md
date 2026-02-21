# Coze Bootstrap API 驗收測試

## 📋 測試前準備

### 1. 設定環境變數

```bash
# Windows (PowerShell)
$env:COZE_BOT_ID="7469370888888888888"
$env:COZE_PAT="pat_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
$env:COZE_CHAT_SDK_SRC="https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/libs/oversea/index.js"

# Linux/Mac (Bash)
export COZE_BOT_ID="7469370888888888888"
export COZE_PAT="pat_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
export COZE_CHAT_SDK_SRC="https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/libs/oversea/index.js"
```

### 2. 重啟後端

```bash
cd backend
./mvnw spring-boot:run
```

---

## ✅ 測試案例

### Test Case 1：正常情況（有完整配置）

**請求：**
```bash
curl -i http://localhost:8080/api/support/coze/bootstrap
```

**預期回應：**
```
HTTP/1.1 200 
Content-Type: application/json

{
  "botId": "7469370888888888888",
  "token": "pat_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "sdkSrc": "https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/libs/oversea/index.js",
  "expiresIn": 0,
  "serverTime": "2026-01-31T14:30:00",
  "note": "⚠️ PAT Token 提醒：此 Token 有過期時間，過期後需要重新建立"
}
```

**後端 Log 應顯示：**
```
✅ Coze Bootstrap 配置已提供 - Bot ID: 7469370888888888888, SDK Src: https://lf-cdn.coze.cn/obj/unpkg/...
```

---

### Test Case 2：缺少 Bot ID

**準備：**
```bash
# Windows
$env:COZE_BOT_ID=""

# Linux/Mac
unset COZE_BOT_ID
```

**請求：**
```bash
curl -i http://localhost:8080/api/support/coze/bootstrap
```

**預期回應：**
```
HTTP/1.1 500 
Content-Type: application/json

{
  "message": "Coze 配置不完整：缺少 Bot ID",
  "hint": "請在 application.yml 設定 coze.bot-id 或環境變數 COZE_BOT_ID",
  "status": 500,
  "timestamp": "2026-01-31T14:30:00"
}
```

**後端 Log 應顯示：**
```
❌ Coze Bot ID 未設定！請在 application.yml 設定 coze.bot-id 或環境變數 COZE_BOT_ID
```

---

### Test Case 3：缺少 PAT Token

**準備：**
```bash
# Windows
$env:COZE_PAT=""

# Linux/Mac
unset COZE_PAT
```

**請求：**
```bash
curl -i http://localhost:8080/api/support/coze/bootstrap
```

**預期回應：**
```
HTTP/1.1 500 
Content-Type: application/json

{
  "message": "Coze 配置不完整：缺少 PAT Token",
  "hint": "請在 application.yml 設定 coze.pat 或環境變數 COZE_PAT",
  "status": 500,
  "timestamp": "2026-01-31T14:30:00"
}
```

**後端 Log 應顯示：**
```
❌ Coze PAT Token 未設定！請在 application.yml 設定 coze.pat 或環境變數 COZE_PAT
```

---

### Test Case 4：測試 404 錯誤修正

**請求不存在的資源：**
```bash
curl -i http://localhost:8080/api/nonexistent/endpoint
```

**預期回應：**
```
HTTP/1.1 404 
Content-Type: application/json

{
  "message": "Not Found",
  "path": "/api/nonexistent/endpoint",
  "status": 404,
  "timestamp": "2026-01-31T14:30:00"
}
```

**重要：不應該是 500 錯誤！**

---

### Test Case 5：CORS 測試（前端呼叫）

**前端 Console 測試：**
```javascript
// 在瀏覽器 Console 執行
fetch('http://localhost:8080/api/support/coze/bootstrap')
  .then(res => res.json())
  .then(data => console.log('✅ CORS 正常:', data))
  .catch(err => console.error('❌ CORS 失敗:', err))
```

**預期：**
- 不應出現 CORS 錯誤
- 能成功取得 JSON 回應

---

## 🔒 安全性檢查

### 檢查點 1：Token 不應出現在 Log

```bash
# 搜尋後端 log，不應出現完整 token
grep "pat_" backend.log
```

**預期：不應出現完整的 `pat_xxxxx...` 字串**

### 檢查點 2：toString() 遮蔽測試

在 Controller 中測試：
```java
log.info("Response: {}", response.toString());
```

**預期 Log：**
```
Response: CozeBootstrapResponseDto{botId='7469370888888888888', token='pat_xxxxxx...', sdkSrc='...', ...}
```

---

## 📊 驗收通過標準

- ✅ Test Case 1-3 回應正確
- ✅ Test Case 4 回傳 404 而非 500
- ✅ Test Case 5 無 CORS 錯誤
- ✅ 安全性檢查通過（Log 不含完整 token）
- ✅ 前端可成功呼叫並取得配置
- ✅ Coze 泡泡可正常初始化

---

## 🐛 常見問題排查

### 問題 1：仍出現 api/api/support 路徑

**原因：** 前端 API 路徑錯誤  
**解決：** 確認 `frontend/src/api/modules/support.js` 使用 `/support/coze/bootstrap` 而非 `/api/support/...`

### 問題 2：CORS 錯誤

**原因：** SecurityConfig 未正確配置  
**解決：** 確認 `SecurityConfig.java` 中：
- `/api/**` 已在 permitAll()
- CORS 配置包含 `http://localhost:5173`

### 問題 3：404 變成 500

**原因：** GlobalExceptionHandler 未處理 NoResourceFoundException  
**解決：** 確認已新增 `handleNoResourceFoundException()` 方法

### 問題 4：Token 外洩在 Log

**原因：** 使用了 `log.info("token: {}", token)`  
**解決：** 確認所有 log 都不直接印 token，使用 DTO 的 toString() 自動遮蔽
