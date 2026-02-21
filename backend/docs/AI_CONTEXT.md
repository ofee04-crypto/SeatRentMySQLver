# 🤖 AI Context - 程式碼索引與快速定位

> 供 Code Review 與 AI 輔助開發使用的檔案索引

---

## 📂 後端 Controller 索引

### 會員模組 (Member)

| Controller                                                                                         | 路徑                 | API 前綴   |
| -------------------------------------------------------------------------------------------------- | -------------------- | ---------- |
| [`MemberController`](../src/main/java/com/example/backend/controller/member/MemberController.java) | `controller/member/` | `/members` |

**主要方法**:

- `findAll()` → GET `/members`
- `findOne()` → GET `/members/find`
- `insert()` → POST `/members`
- `update()` → POST `/members/update`
- `delete()` → GET `/members/delete`

---

### 據點模組 (Spot)

| Controller                                                                                               | 路徑               | API 前綴 |
| -------------------------------------------------------------------------------------------------------- | ------------------ | -------- |
| [`SpotListServ`](../src/main/java/com/example/backend/controller/spot/SpotListServ.java)                 | `controller/spot/` | `/spot`  |
| [`RentalSpotController`](../src/main/java/com/example/backend/controller/spot/RentalSpotController.java) | `controller/spot/` | `/spot`  |

---

### 座位模組 (Seat)

| Controller                                                                                   | 路徑               | API 前綴 |
| -------------------------------------------------------------------------------------------- | ------------------ | -------- |
| [`SeatListServ`](../src/main/java/com/example/backend/controller/spot/SeatListServ.java)     | `controller/spot/` | `/seat`  |
| [`SeatController`](../src/main/java/com/example/backend/controller/spot/SeatController.java) | `controller/spot/` | `/seat`  |

---

### 維修模組 (Maintenance)

| Controller                                                                                                        | 路徑                      | API 前綴           |
| ----------------------------------------------------------------------------------------------------------------- | ------------------------- | ------------------ |
| [`MaintenanceController`](../src/main/java/com/example/backend/controller/maintenance/MaintenanceController.java) | `controller/maintenance/` | `/api/maintenance` |

**工單狀態流轉 API**:

- `POST /api/maintenance/tickets/{id}/assign` - 指派人員
- `POST /api/maintenance/tickets/{id}/start` - 開始維修
- `POST /api/maintenance/tickets/{id}/resolve` - 結案
- `POST /api/maintenance/tickets/{id}/cancel` - 取消

---

### 租借訂單模組 (Rec)

| Controller                                                                                                | 路徑              | API 前綴            |
| --------------------------------------------------------------------------------------------------------- | ----------------- | ------------------- |
| [`RecRentController`](../src/main/java/com/example/backend/controller/rec/RecRentController.java)         | `controller/rec/` | `/api/rec-rents`    |
| [`RentDetailsController`](../src/main/java/com/example/backend/controller/rec/RentDetailsController.java) | `controller/rec/` | `/api/rent-details` |

---

### 商家與優惠券模組 (MerchantAndCoupon)

| Controller                                                                                                        | 路徑                            | API 前綴         |
| ----------------------------------------------------------------------------------------------------------------- | ------------------------------- | ---------------- |
| [`MerchantController`](../src/main/java/com/example/backend/controller/merchantAndCoupon/MerchantController.java) | `controller/merchantAndCoupon/` | `/api/merchants` |
| [`DiscountController`](../src/main/java/com/example/backend/controller/merchantAndCoupon/DiscountController.java) | `controller/merchantAndCoupon/` | `/api/discounts` |

---

## 📂 後端 Entity 索引

| Entity                                                                                                         | 路徑                       | 對應資料表               |
| -------------------------------------------------------------------------------------------------------------- | -------------------------- | ------------------------ |
| [`Member`](../src/main/java/com/example/backend/model/member/Member.java)                                      | `model/member/`            | `member`                 |
| [`Admin`](../src/main/java/com/example/backend/model/member/Admin.java)                                        | `model/member/`            | `admin`                  |
| [`RentalSpot`](../src/main/java/com/example/backend/model/spot/RentalSpot.java)                                | `model/spot/`              | `renting_Spot`           |
| [`Seat`](../src/main/java/com/example/backend/model/spot/Seat.java)                                            | `model/spot/`              | `seats`                  |
| [`RecRent`](../src/main/java/com/example/backend/model/rec/RecRent.java)                                       | `model/rec/`               | `recRent`                |
| [`RentDetails`](../src/main/java/com/example/backend/model/rec/RentDetails.java)                               | `model/rec/`               | (View)                   |
| [`Merchant`](../src/main/java/com/example/backend/model/merchantAndCoupon/Merchant.java)                       | `model/merchantAndCoupon/` | `merchant`               |
| [`Discount`](../src/main/java/com/example/backend/model/merchantAndCoupon/Discount.java)                       | `model/merchantAndCoupon/` | `discount`               |
| [`MaintenanceStaff`](../src/main/java/com/example/backend/model/maintenance/MaintenanceStaff.java)             | `model/maintenance/`       | `maintenanceStaff`       |
| [`MaintenanceInformation`](../src/main/java/com/example/backend/model/maintenance/MaintenanceInformation.java) | `model/maintenance/`       | `maintenanceInformation` |

---

## 📂 後端 Service 索引

| Service                                                                                                                        | 路徑                   | 對應 Controller       |
| ------------------------------------------------------------------------------------------------------------------------------ | ---------------------- | --------------------- |
| [`MemberService`](../src/main/java/com/example/backend/service/member/MemberService.java)                                      | `service/member/`      | MemberController      |
| [`RentalSpotService`](../src/main/java/com/example/backend/service/spot/RentalSpotService.java)                                | `service/spot/`        | SpotListServ          |
| [`SeatService`](../src/main/java/com/example/backend/service/spot/SeatService.java)                                            | `service/spot/`        | SeatListServ          |
| [`MaintenanceInformationService`](../src/main/java/com/example/backend/service/maintenance/MaintenanceInformationService.java) | `service/maintenance/` | MaintenanceController |
| [`MaintenanceStaffService`](../src/main/java/com/example/backend/service/maintenance/MaintenanceStaffService.java)             | `service/maintenance/` | MaintenanceController |

---

## 📂 後端 Repository 索引

| Repository                                                                                                                              | 路徑                      |
| --------------------------------------------------------------------------------------------------------------------------------------- | ------------------------- |
| [`MemberRepository`](../src/main/java/com/example/backend/repository/member/MemberRepository.java)                                      | `repository/member/`      |
| [`RentalSpotRepository`](../src/main/java/com/example/backend/repository/spot/RentalSpotRepository.java)                                | `repository/spot/`        |
| [`SeatRepository`](../src/main/java/com/example/backend/repository/spot/SeatRepository.java)                                            | `repository/spot/`        |
| [`RecRentRepository`](../src/main/java/com/example/backend/repository/rec/RecRentRepository.java)                                       | `repository/rec/`         |
| [`RentDetailsRepository`](../src/main/java/com/example/backend/repository/rec/RentDetailsRepository.java)                               | `repository/rec/`         |
| [`MaintenanceInformationRepository`](../src/main/java/com/example/backend/repository/maintenance/MaintenanceInformationRepository.java) | `repository/maintenance/` |
| [`MaintenanceStaffRepository`](../src/main/java/com/example/backend/repository/maintenance/MaintenanceStaffRepository.java)             | `repository/maintenance/` |

---

## 📂 後端設定檔索引

| 檔案                                                                             | 路徑      | 說明                     |
| -------------------------------------------------------------------------------- | --------- | ------------------------ |
| [`WebConfig`](../src/main/java/com/example/backend/config/WebConfig.java)        | `config/` | CORS、靜態資源、健康檢查 |
| [`DbPrintRunner`](../src/main/java/com/example/backend/utils/DbPrintRunner.java) | `utils/`  | 資料庫列印工具 (開發用)  |

---

## 📂 前端 Router 索引

> 檔案位置: [`frontend/src/router/index.js`](../../frontend/src/router/index.js)

### 公開路由

| Path     | Name | Component         |
| -------- | ---- | ----------------- |
| `/login` | -    | `LoginView.vue`   |
| `/`      | -    | 重導向至 `/login` |

### 後台路由 (`/admin/*`)

| Path                      | Name            | Component                     | 說明       |
| ------------------------- | --------------- | ----------------------------- | ---------- |
| `/admin`                  | `admin-home`    | `AdminHomeView.vue`           | 後台首頁   |
| `/admin/spot/list`        | `spot-list`     | `SpotList.vue`                | 據點列表   |
| `/admin/spot/add`         | `spot-add`      | `SpotForm.vue`                | 新增據點   |
| `/admin/spot/edit/:id`    | `spot-edit`     | `SpotForm.vue`                | 編輯據點   |
| `/admin/seat/list`        | `seat-list`     | `SeatList.vue`                | 座位列表   |
| `/admin/seat/insert`      | `seat-insert`   | `SeatInsert.vue`              | 新增座位   |
| `/admin/seat/edit/:id`    | `seat-edit`     | `SeatUpdate.vue`              | 編輯座位   |
| `/admin/seat/view/:id`    | `seat-view`     | `SeatOne.vue`                 | 座位詳情   |
| `/admin/seat/search`      | `seat-search`   | `SeatSearch.vue`              | 座位查詢   |
| `/admin/seat/result`      | `seat-result`   | `SeatResult.vue`              | 查詢結果   |
| `/admin/merchants`        | `merchants`     | `MerchantList.vue`            | 商家管理   |
| `/admin/discounts`        | `discounts`     | `DiscountList.vue`            | 優惠券管理 |
| `/admin/members`          | `member-list`   | `MemberListView.vue`          | 會員列表   |
| `/admin/members/edit/:id` | `member-edit`   | `MemberEditView.vue`          | 編輯會員   |
| `/admin/members/create`   | `member-create` | `MemberCreateView.vue`        | 新增會員   |
| `/admin/rec-rent`         | `rec-rent`      | `RecRentManagement.vue`       | 租借訂單   |
| `/admin/staff-list`       | `staff-list`    | `MaintenanceStaffList.vue`    | 維護人員   |
| `/admin/staff-form/:id?`  | `staff-form`    | `MaintenanceStaffForm.vue`    | 人員表單   |
| `/admin/staff-history`    | `staff-history` | `MaintenanceStaffHistory.vue` | 人員紀錄   |
| `/admin/mtif-list`        | `mtif-list`     | `MtifList.vue`                | 工單列表   |
| `/admin/mtif-history`     | `mtif-history`  | `MtifList.vue`                | 工單歷史   |
| `/admin/mtif-form/:id?`   | `mtif-form`     | `MtifForm.vue`                | 工單表單   |

---

## 📂 前端 Views 索引

### 會員模組 (`views/member/`)

| 檔案                                                                           | 說明                |
| ------------------------------------------------------------------------------ | ------------------- |
| [`AdminLayout.vue`](../../frontend/src/views/member/AdminLayout.vue)           | AdminLTE 3 後台框架 |
| [`AdminHomeView.vue`](../../frontend/src/views/member/AdminHomeView.vue)       | 後台首頁儀表板      |
| [`LoginView.vue`](../../frontend/src/views/member/LoginView.vue)               | 登入頁面            |
| [`MemberListView.vue`](../../frontend/src/views/member/MemberListView.vue)     | 會員列表            |
| [`MemberEditView.vue`](../../frontend/src/views/member/MemberEditView.vue)     | 編輯會員            |
| [`MemberCreateView.vue`](../../frontend/src/views/member/MemberCreateView.vue) | 新增會員            |

### 據點模組 (`views/spot/`)

| 檔案                                                             | 說明                 |
| ---------------------------------------------------------------- | -------------------- |
| [`SpotList.vue`](../../frontend/src/views/spot/SpotList.vue)     | 據點列表             |
| [`SpotForm.vue`](../../frontend/src/views/spot/SpotForm.vue)     | 據點表單 (新增/編輯) |
| [`SpotOne.vue`](../../frontend/src/views/spot/SpotOne.vue)       | 據點詳情             |
| [`SeatList.vue`](../../frontend/src/views/spot/SeatList.vue)     | 座位列表             |
| [`SeatInsert.vue`](../../frontend/src/views/spot/SeatInsert.vue) | 新增座位             |
| [`SeatUpdate.vue`](../../frontend/src/views/spot/SeatUpdate.vue) | 編輯座位             |
| [`SeatOne.vue`](../../frontend/src/views/spot/SeatOne.vue)       | 座位詳情             |
| [`SeatSearch.vue`](../../frontend/src/views/spot/SeatSearch.vue) | 座位查詢             |
| [`SeatResult.vue`](../../frontend/src/views/spot/SeatResult.vue) | 查詢結果             |

### 維修模組 (`views/maintenance/`)

| 檔案                                                                                              | 說明                    |
| ------------------------------------------------------------------------------------------------- | ----------------------- |
| [`MtifList.vue`](../../frontend/src/views/maintenance/MtifList.vue)                               | 工單列表 (支援歷史模式) |
| [`MtifForm.vue`](../../frontend/src/views/maintenance/MtifForm.vue)                               | 工單表單                |
| [`MaintenanceStaffList.vue`](../../frontend/src/views/maintenance/MaintenanceStaffList.vue)       | 人員列表                |
| [`MaintenanceStaffForm.vue`](../../frontend/src/views/maintenance/MaintenanceStaffForm.vue)       | 人員表單                |
| [`MaintenanceStaffHistory.vue`](../../frontend/src/views/maintenance/MaintenanceStaffHistory.vue) | 人員歷史                |

### 租借訂單模組 (`views/rec/`)

| 檔案                                                                          | 說明         |
| ----------------------------------------------------------------------------- | ------------ |
| [`RecRentManagement.vue`](../../frontend/src/views/rec/RecRentManagement.vue) | 訂單管理主頁 |

### 租借訂單元件 (`components/rec/`)

| 檔案                                                                                   | 說明         |
| -------------------------------------------------------------------------------------- | ------------ |
| [`RecRentAdd.vue`](../../frontend/src/components/rec/RecRentAdd.vue)                   | 新增訂單表單 |
| [`RecRentUserOrder.vue`](../../frontend/src/components/rec/RecRentUserOrder.vue)       | 使用者下單   |
| [`RecRentUserComplete.vue`](../../frontend/src/components/rec/RecRentUserComplete.vue) | 使用者歸還   |

### 商家與優惠券模組 (`views/merchantAndCoupon/`)

| 檔案                                                                              | 說明       |
| --------------------------------------------------------------------------------- | ---------- |
| [`MerchantList.vue`](../../frontend/src/views/merchantAndCoupon/MerchantList.vue) | 商家管理   |
| [`DiscountList.vue`](../../frontend/src/views/merchantAndCoupon/DiscountList.vue) | 優惠券管理 |

---

## 📂 前端 API 模組索引

### 核心設定

| 檔案                                         | 路徑       | 說明                   |
| -------------------------------------------- | ---------- | ---------------------- |
| [`Axios.js`](../../frontend/src/js/Axios.js) | `src/js/`  | Axios 全域設定、攔截器 |
| [`http.js`](../../frontend/src/api/http.js)  | `src/api/` | HTTP 客戶端封裝 (預留) |

### Vite Proxy 設定

> 檔案位置: [`frontend/vite.config.js`](../../frontend/vite.config.js)

| Proxy Path | Target                  | 說明                        |
| ---------- | ----------------------- | --------------------------- |
| `/login`   | `http://localhost:8080` | 登入請求                    |
| `/seat`    | `http://localhost:8080` | 座位 API                    |
| `/api`     | `http://localhost:8080` | 通用 API (移除 `/api` 前綴) |
| `/images`  | `http://localhost:8080` | 圖片資源                    |

---

## 🔄 狀態流轉參考

### 維修工單狀態 (`issueStatus`)

```
REPORTED (已通報)
    │
    ▼ [assign]
ASSIGNED (已指派)
    │
    ▼ [start]
UNDER_MAINTENANCE (維修中)
    │
    ├──▶ [resolve] ──▶ RESOLVED (已完成)
    │
    └──▶ [cancel] ──▶ CANCELLED (已取消)
```

### 工單優先級 (`issuePriority`)

| 值       | 中文 | 說明       |
| -------- | ---- | ---------- |
| `LOW`    | 低   | 可延後處理 |
| `NORMAL` | 普通 | 一般處理   |
| `HIGH`   | 高   | 優先處理   |
| `URGENT` | 緊急 | 立即處理   |

### 租借訂單狀態 (`recStatus`)

| 值       | 說明         |
| -------- | ------------ |
| `租借中` | 進行中的租借 |
| `已完成` | 已歸還       |
| `已取消` | 訂單取消     |

---

## 🛠️ 開發快速指令

### 後端

```bash
# 執行測試
./mvnw test

# 打包
./mvnw package -DskipTests

# 清除並重建
./mvnw clean install
```

### 前端

```bash
# 開發模式
npm run dev

# 建置生產版本
npm run build

# ESLint 檢查
npm run lint

# 格式化程式碼
npm run format
```

---

## 📋 Code Review Checklist

### 後端

- [ ] Controller 是否加上 `@CrossOrigin` 或使用全域 CORS
- [ ] 是否使用建構子注入取代 `@Autowired`
- [ ] Service 層是否包含業務邏輯
- [ ] Repository 是否只處理資料存取
- [ ] 例外處理是否完整

### 前端

- [ ] 是否移除未使用變數 (ESLint)
- [ ] API 呼叫是否使用 `/api` 前綴
- [ ] 是否處理 loading 與 error 狀態
- [ ] 元件是否使用 Composition API
- [ ] 路由是否正確設定

---

## 🔗 相關文件

- [專案架構總覽](./PROJECT_MAP.md)
- [資料庫腳本](../db/all/第三組SQ資料.sql)
- [README](../../README.md)
