# Coze SDK 本地託管

此目錄用於存放 Coze Chat SDK 的本地副本，作為 script loader 的備用來源。

## 設置步驟

1. 從 CDN 下載 SDK：
   ```bash
   # 使用 curl
   curl -o index.js "https://lf-cdn.coze.com/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/index.js"
   
   # 或使用 wget
   wget -O index.js "https://lf-cdn.coze.com/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/index.js"
   ```

2. 將下載的 `index.js` 放置在此目錄

3. 驗證可訪問性：
   - 啟動後端服務後，訪問 `http://localhost:8080/vendor/coze/index.js`
   - 應該能看到 JavaScript 內容

## 版本更新

建議定期從 Coze 官方 CDN 更新 SDK 版本。當前推薦版本：

- **1.0.0-beta.4** - 穩定版本
- **1.2.0-beta.3** - 最新 beta（可能有新功能但不穩定）

## 使用方式

在 `.env` 中設定：

```env
# 使用本地託管
VITE_COZE_SDK_MODE="script"
VITE_COZE_CHAT_SDK_SRC="/vendor/coze/index.js"

# 或自動模式（npm 失敗後 fallback）
VITE_COZE_SDK_MODE="auto"
```

## 注意事項

- 此目錄下的 `index.js` 不會被 Git 追蹤（已加入 .gitignore）
- 每次部署時需手動下載或透過 CI/CD 自動下載
- 確保後端服務正確配置靜態資源路徑
