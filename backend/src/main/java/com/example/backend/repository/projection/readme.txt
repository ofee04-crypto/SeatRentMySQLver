=============================================================================
 Package: com.example.backend.repository.projection
 說明: Spring Data JPA Projections (投影介面) 存放區
=============================================================================

1. 用途 (Purpose)
-----------------------------------------------------------------------------
   此 Package 專門存放「介面 (Interface)」，用於接收 Repository 層中自定義 SQL 
   (@Query) 的查詢結果。

   當查詢結果是跨多個 Table 的統計數據、或是只選取部分欄位，無法直接對應到單一 
   Entity (如 Member, RentingSpot) 時，就需要使用這裡的 Projection 介面來接收。

2. 命名與對應規則 (Naming Convention)
-----------------------------------------------------------------------------
   Spring Data JPA 使用「別名對應 (Alias Mapping)」機制。
   介面中的 Getter 方法名稱，必須與 SQL 查詢語句中的 AS 別名完全一致 (區分大小寫)。

   [範例]
   SQL: 
     SELECT s.spot_name AS spotName, COUNT(*) AS totalCount FROM ...
   
   Projection Interface:
     String getSpotName();   // 對應 AS spotName
     Integer getTotalCount(); // 對應 AS totalCount

3. 參考文件
-----------------------------------------------------------------------------
   Spring Data JPA - Projections
   https://docs.spring.io/spring-data/jpa/reference/repositories/projections.html

4. 自動檢查機制 (Validation)
-----------------------------------------------------------------------------
   由於 Native Query 的別名錯誤通常在執行時才會發現，本專案已配置啟動檢查器：
   位置: com.example.backend.config.ProjectionValidationConfig
   
   在開發環境 (dev profile) 啟動時，系統會自動試跑 Dashboard 相關查詢。
   若 SQL AS 別名與介面 Getter 不匹配，Console 會出現 ❌ [FAIL] 錯誤日誌。