# 🗺️ SeatRentSys 專案架構總覽

> 最後更新：2025-01

---

## 📁 目錄結構

```
SeatRentSys/
├── backend/                    # Spring Boot 後端
│   ├── src/main/java/com/example/backend/
│   │   ├── config/             # 全域設定 (CORS、靜態資源)
│   │   ├── controller/         # REST API 控制器
│   │   ├── model/              # JPA Entity 實體類別
│   │   ├── repository/         # Spring Data JPA Repository
│   │   ├── service/            # 業務邏輯層
│   │   ├── integration/        # 外部 API 整合 (預留)
│   │   └── utils/              # 工具類別
│   ├── db/                     # SQL 腳本
│   │   ├── all/                # 完整資料庫建立腳本
│   │   ├── schema/             # 表結構定義
│   │   └── seed/               # 測試資料
│   └── docs/                   # 後端文件
│
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── api/                # Axios 封裝與模組化 API
│   │   ├── assets/             # 靜態資源 (CSS)
│   │   ├── components/         # 可重用元件
│   │   ├── router/             # Vue Router 路由設定
│   │   ├── views/              # 頁面元件
│   │   │   ├── member/         # 會員與後台框架
│   │   │   ├── spot/           # 據點與座位管理
│   │   │   ├── rec/            # 租借訂單管理
│   │   │   ├── maintenance/    # 維修管理
│   │   │   └── merchantAndCoupon/ # 商家與優惠券
│   │   └── main.js             # Vue 應用程式入口
│   └── public/vendor/          # AdminLTE 靜態資源
│
├── uploads/                    # 圖片上傳目錄
└── images/                     # 圖片資源
```

---

## 🚀 啟動方式

### 後端 (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run
# 或使用 IDE 執行 BackendApplication.java
```

- **預設埠號**: `8080`
- **健康檢查**: `GET http://localhost:8080/test`

### 前端 (Vue 3 + Vite)

```bash
cd frontend
npm install
npm run dev
```

- **預設埠號**: `5173`
- **首頁**: `http://localhost:5173/login`

---

## ⚙️ 環境變數與設定

### 後端 (`application.yml`)

| 變數名稱                     | 說明                | 預設值       |
| ---------------------------- | ------------------- | ------------ |
| `spring.datasource.url`      | SQL Server 連線字串 | -            |
| `spring.datasource.username` | 資料庫帳號          | -            |
| `spring.datasource.password` | 資料庫密碼          | -            |
| `app.file.upload-path`       | 圖片上傳路徑        | `./uploads/` |
| `app.tools.db-print`         | 啟用資料庫列印工具  | `false`      |

### 前端 (`vite.config.js`)

| 設定項                 | 說明                                  |
| ---------------------- | ------------------------------------- |
| `server.proxy./api`    | 代理至後端 8080，自動移除 `/api` 前綴 |
| `server.proxy./login`  | 登入請求代理                          |
| `server.proxy./seat`   | 座位 API 代理                         |
| `server.proxy./images` | 圖片資源代理                          |

---

## 📦 主要模組

### 1. 會員管理 (Member)

- **功能**: 會員 CRUD、登入驗證
- **後端**: `/members/*`
- **前端**: `/admin/members`

### 2. 據點管理 (Spot)

- **功能**: 租借點位 CRUD、狀態管理
- **後端**: `/api/spot/*`
- **前端**: `/admin/spot/list`

### 3. 座位管理 (Seat)

- **功能**: 設備 CRUD、條件查詢
- **後端**: `/api/seat/*`
- **前端**: `/admin/seat/list`

### 4. 維修管理 (Maintenance)

- **功能**: 工單 CRUD、狀態流轉、人員指派
- **後端**: `/api/maintenance/*`
- **前端**: `/admin/mtif-list`, `/admin/staff-list`

### 5. 租借訂單 (RecRent)

- **功能**: 訂單 CRUD、使用者下單與歸還
- **後端**: `/api/rec-rents/*`
- **前端**: `/admin/rec-rent`

### 6. 商家與優惠券 (Merchant & Discount)

- **功能**: 商家 CRUD、優惠券管理
- **後端**: `/api/merchants/*`, `/api/discounts/*`
- **前端**: `/admin/merchants`, `/admin/discounts`

---

## 🔌 主要 API 端點

### 會員 API

| Method | Endpoint                     | 說明         |
| ------ | ---------------------------- | ------------ |
| GET    | `/members`                   | 查詢全部會員 |
| GET    | `/members/find?memId={id}`   | 查詢單筆會員 |
| POST   | `/members`                   | 新增會員     |
| POST   | `/members/update`            | 更新會員     |
| GET    | `/members/delete?memId={id}` | 刪除會員     |

### 據點 API

| Method | Endpoint            | 說明         |
| ------ | ------------------- | ------------ |
| GET    | `/spot/list`        | 查詢全部據點 |
| GET    | `/spot/condition`   | 條件查詢據點 |
| POST   | `/spot/insert`      | 新增據點     |
| PUT    | `/spot/update`      | 更新據點     |
| DELETE | `/spot/delete/{id}` | 刪除據點     |

### 座位 API

| Method | Endpoint            | 說明         |
| ------ | ------------------- | ------------ |
| GET    | `/seat/list`        | 查詢全部座位 |
| GET    | `/seat/condition`   | 條件查詢座位 |
| POST   | `/seat/insert`      | 新增座位     |
| PUT    | `/seat/update`      | 更新座位     |
| DELETE | `/seat/delete/{id}` | 刪除座位     |

### 維修 API

| Method | Endpoint                                | 說明           |
| ------ | --------------------------------------- | -------------- |
| GET    | `/api/maintenance/staff`                | 查詢維護人員   |
| POST   | `/api/maintenance/staff`                | 新增維護人員   |
| DELETE | `/api/maintenance/staff/{id}`           | 刪除維護人員   |
| GET    | `/api/maintenance/tickets`              | 查詢全部工單   |
| GET    | `/api/maintenance/tickets/active`       | 查詢待處理工單 |
| GET    | `/api/maintenance/tickets/history`      | 查詢歷史工單   |
| POST   | `/api/maintenance/tickets`              | 新增工單       |
| PUT    | `/api/maintenance/tickets/{id}`         | 更新工單       |
| POST   | `/api/maintenance/tickets/{id}/assign`  | 指派人員       |
| POST   | `/api/maintenance/tickets/{id}/start`   | 開始維修       |
| POST   | `/api/maintenance/tickets/{id}/resolve` | 結案           |
| POST   | `/api/maintenance/tickets/{id}/cancel`  | 取消工單       |

### 租借訂單 API

| Method | Endpoint                 | 說明           |
| ------ | ------------------------ | -------------- |
| GET    | `/api/rec-rents`         | 查詢全部訂單   |
| GET    | `/api/rent-details/all`  | 查詢租借明細   |
| GET    | `/api/rent-details/{id}` | 依 ID 查詢明細 |

---

## 🗄️ 資料表概要

### 核心表格

| 表格名稱                 | 說明       | 主鍵                |
| ------------------------ | ---------- | ------------------- |
| `member`                 | 會員資料   | `memId`             |
| `admin`                  | 管理員資料 | `admId`             |
| `renting_Spot`           | 租借據點   | `spotId`            |
| `seats`                  | 座位設備   | `seatsId`           |
| `recRent`                | 租借訂單   | `recSeqId`, `recId` |
| `merchant`               | 合作商家   | `merchantId`        |
| `discount`               | 優惠券     | `couponId`          |
| `maintenanceStaff`       | 維護人員   | `staffId`           |
| `maintenanceInformation` | 維修工單   | `ticketId`          |

### 表格關聯

```
member ─────┬──< recRent >──┬───── seats
            │               │
            │               └───── renting_Spot ──< merchant
            │
maintenanceStaff ──< maintenanceInformation >── renting_Spot
                                               │
discount ──────────────────────────────────────┘
```

### 主要欄位說明

#### `recRent` (租借訂單)

- `recId`: 自動生成流水號 (格式: R000000001)
- `recStatus`: 訂單狀態 (租借中/已完成/已取消)
- `recRentDT2` / `recReturnDT2`: 租借/歸還時間

#### `maintenanceInformation` (維修工單)

- `issueStatus`: 狀態流轉 (REPORTED → ASSIGNED → UNDER_MAINTENANCE → RESOLVED/CANCELLED)
- `issuePriority`: 優先級 (LOW/NORMAL/HIGH/URGENT)

---

## 🔐 CORS 設定

後端允許前端 `http://localhost:5173` 跨域存取，設定於：

- `WebConfig.java` - 全域 CORS
- 各 Controller `@CrossOrigin` 註解

---

## 📝 開發注意事項

1. **API 前綴**: 前端呼叫時加上 `/api`，Vite 會自動代理並移除
2. **圖片上傳**: 存放於 `uploads/`，透過 `/images/**` 存取
3. **建構子注入**: 推薦使用建構子注入取代 `@Autowired`
4. **ESLint**: 前端已啟用 ESLint，請確保無未使用變數
