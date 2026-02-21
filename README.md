# SeatRentSys - 共享座位預約系統

## 📖 專案簡介 (Project Description)

這是一個基於前後端分離架構開發的「共享隨處座椅系統」。
使用者可以透過此系統尋找並預約閒置座位，商家則可以管理座位與訂單。

## 🛠️ 技術堆疊 (Tech Stack)

### Backend (後端)

- **Language**: Java 17+
- **Framework**: Spring Boot 3
- **ORM**: Hibernate / Spring Data JPA
- **Database**: SQL Server
- **Build Tool**: Maven

### Frontend (前端)

- **Framework**: Vue 3 (Composition API)
- **Build Tool**: Vite
- **Language**: TypeScript / JavaScript

## 🚀 功能模組

1.  **會員系統**: 註冊、登入、個人資料管理
2.  **座位預約**: 搜尋、預約、取消
3.  **商家後台**: 座位管理、報表查看

## ⚙️ 如何執行 (Setup)

1.  Clone 此專案
2.  進入 `backend` 資料夾，設定 `application.yml` 資料庫連線
3.  執行 Spring Boot Application
4.  進入 `frontend` 資料夾，執行 `npm install` 與 `npm run dev`
