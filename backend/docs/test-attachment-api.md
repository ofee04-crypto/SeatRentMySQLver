# 工單圖片附件 API 測試指南

## 📋 新增/修改檔案清單

### 新增檔案
1. ✅ `backend/src/main/java/com/example/backend/model/maintenance/MaintenanceTicketAttachment.java` - Entity
2. ✅ `backend/src/main/java/com/example/backend/repository/maintenance/MaintenanceTicketAttachmentRepository.java` - Repository
3. ✅ `backend/src/main/java/com/example/backend/dto/maintenance/AttachmentResponseDto.java` - DTO
4. ✅ `backend/src/main/java/com/example/backend/service/maintenance/AttachmentService.java` - Service

### 修改檔案
1. ✅ `backend/src/main/java/com/example/backend/controller/maintenance/MaintenanceController.java` - 新增附件相關 API

---

## 🎯 API 端點說明

### 1. 上傳附件（多檔）

**端點**: `POST /api/maintenance/tickets/{ticketId}/attachments`

**參數**:
- `ticketId` (路徑參數) - 工單 ID（必須存在於 maintenanceInformation）
- `files` (multipart/form-data) - 圖片檔案（可多個，最多 5 張）
- `note` (form field, 可選) - 備註（套用到所有上傳的附件）

**限制**:
- 單檔最大 5MB
- 每次最多上傳 5 張
- 僅支援圖片格式：`image/jpeg`, `image/png`, `image/webp`

**回應範例**:
```json
[
  {
    "attachmentId": 1,
    "ticketId": 1,
    "originalName": "repair-photo.jpg",
    "storedName": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
    "contentType": "image/jpeg",
    "fileSize": 1024567,
    "publicUrl": "/images/maintenance/tickets/1/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
    "sortOrder": 1,
    "note": "維修現場照片",
    "createdAt": "2026-01-31 12:00:00"
  }
]
```

---

### 2. 查詢附件清單

**端點**: `GET /api/maintenance/tickets/{ticketId}/attachments`

**參數**:
- `ticketId` (路徑參數) - 工單 ID

**回應**: 只回傳 `isActive=1` 的附件，排序為 `sortOrder ASC, createdAt DESC`

---

### 3. 刪除附件

**端點**: `DELETE /api/maintenance/attachments/{attachmentId}`

**參數**:
- `attachmentId` (路徑參數) - 附件 ID

**行為**:
- DB: 設定 `isActive = 0`（軟刪除）
- 檔案系統: 嘗試刪除實體檔案（失敗不影響 API 回應，只記 log）

**回應**: 刪除後的附件資訊

---

## 🧪 測試方法

### 方法 1: 使用 curl（PowerShell）

#### 1. 上傳單張圖片
```powershell
curl -X POST http://localhost:8080/api/maintenance/tickets/1/attachments `
  -F "files=@test-image.jpg" `
  -F "note=維修現場照片"
```

#### 2. 上傳多張圖片
```powershell
curl -X POST http://localhost:8080/api/maintenance/tickets/1/attachments `
  -F "files=@photo1.jpg" `
  -F "files=@photo2.png" `
  -F "files=@photo3.webp" `
  -F "note=故障設備照片"
```

#### 3. 查詢附件清單
```powershell
curl -X GET http://localhost:8080/api/maintenance/tickets/1/attachments
```

#### 4. 刪除附件
```powershell
curl -X DELETE http://localhost:8080/api/maintenance/attachments/1
```

---

### 方法 2: 使用 Postman

#### 上傳附件
1. Method: `POST`
2. URL: `http://localhost:8080/api/maintenance/tickets/1/attachments`
3. Body:
   - 選擇 `form-data`
   - 新增 key: `files`，Type: `File`，選擇圖片檔案（可按住 Ctrl 多選）
   - 新增 key: `note`，Type: `Text`，輸入備註（可選）
4. Send

#### 查詢附件
1. Method: `GET`
2. URL: `http://localhost:8080/api/maintenance/tickets/1/attachments`
3. Send

#### 刪除附件
1. Method: `DELETE`
2. URL: `http://localhost:8080/api/maintenance/attachments/1`
3. Send

---

### 方法 3: 使用 PowerShell Invoke-WebRequest

```powershell
# 上傳附件
$file = Get-Item "D:\test-image.jpg"
$form = @{
    files = $file
    note = "測試圖片"
}
Invoke-WebRequest -Uri "http://localhost:8080/api/maintenance/tickets/1/attachments" `
    -Method POST -Form $form

# 查詢附件
Invoke-WebRequest -Uri "http://localhost:8080/api/maintenance/tickets/1/attachments" `
    -Method GET | Select-Object -ExpandProperty Content

# 刪除附件
Invoke-WebRequest -Uri "http://localhost:8080/api/maintenance/attachments/1" `
    -Method DELETE
```

---

## ✅ 驗收檢查清單

### 資料庫驗證
- [ ] 上傳成功後，檢查 `maintenanceTicketAttachment` 表是否有新記錄
```sql
SELECT * FROM dbo.maintenanceTicketAttachment 
WHERE ticketId = 1 AND isActive = 1
ORDER BY sortOrder ASC, createdAt DESC
```

### 檔案系統驗證
- [ ] 檢查檔案是否存在於: `uploads/maintenance/tickets/{ticketId}/{storedName}`
- [ ] 檔名格式為: `{UUID}.{副檔名}` (例如: `a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg`)

### API 功能驗證
- [ ] 上傳不存在的 ticketId 回傳 500（IllegalArgumentException: 找不到工單）
- [ ] 上傳非圖片格式回傳 500（IllegalArgumentException: 不支援的檔案格式）
- [ ] 上傳超過 5MB 的檔案回傳 500（IllegalArgumentException: 檔案大小超過限制）
- [ ] 上傳超過 5 張圖片回傳 500（IllegalArgumentException: 一次最多上傳 5 個檔案）
- [ ] sortOrder 自動遞增（第一張為 1，第二張為 2...）
- [ ] GET 清單只顯示 `isActive=1` 的附件
- [ ] DELETE 後 `isActive=0`，清單不再顯示該附件

### 瀏覽器直接訪問
- [ ] 開啟瀏覽器，訪問 publicUrl（例如: `http://localhost:8080/images/maintenance/tickets/1/xxx.jpg`）
- [ ] 應該可以直接看到圖片（透過 WebConfig 的 `/images/**` 映射）

---

## 🎨 檔案儲存結構

```
uploads/
└── maintenance/
    └── tickets/
        ├── 1/
        │   ├── a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
        │   ├── b2c3d4e5-f6g7-8901-bcde-f12345678901.png
        │   └── c3d4e5f6-g7h8-9012-cdef-123456789012.webp
        ├── 2/
        │   └── d4e5f6g7-h8i9-0123-defg-234567890123.jpg
        └── ...
```

---

## 🔐 安全性說明

1. **檔名防護**: `storedName` 由系統生成（UUID），不直接使用用戶上傳的檔名
2. **路徑穿越防護**: `originalName` 經過 `sanitizeFileName()` 處理，只保留檔案名稱
3. **檔案類型限制**: 只允許 `image/jpeg`, `image/png`, `image/webp`
4. **檔案大小限制**: 單檔最大 5MB
5. **數量限制**: 每次最多上傳 5 張
6. **唯一性保證**: 透過 UUID 生成機制確保滿足 `UX_mta_ticket_storedName` 唯一性約束

---

## 📝 建議 Commit Message

```
feat(maintenance): 新增工單圖片附件功能

- 新增 MaintenanceTicketAttachment Entity/Repository/Service/DTO
- 提供上傳/查詢/刪除附件 REST API（僅支援圖片格式）
- 檔案儲存於 uploads/maintenance/tickets/{ticketId}/
- 支援多檔上傳（最多5張，單檔最大5MB）
- 實作軟刪除機制（isActive flag）
- storedName 使用 UUID 防止重複與路徑穿越攻擊
- publicUrl 透過 /images/** 映射可直接瀏覽器訪問
```

---

## 🚀 後續建議

如需增強功能，可考慮：
1. 新增圖片壓縮（減少儲存空間）
2. 新增縮圖生成（提升載入速度）
3. 支援更多圖片格式（gif, bmp 等）
4. 新增批量刪除 API
5. 新增附件排序調整 API（修改 sortOrder）
6. 整合雲端儲存（Azure Blob Storage / AWS S3）
7. 新增圖片浮水印（標註工單編號）
