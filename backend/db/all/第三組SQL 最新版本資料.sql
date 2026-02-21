--重置資料庫 
--Version Log: 2026/01/21
--===========CLEAR=================
USE SeatRentSys
DROP TABLE recRent;
DROP TABLE maintenanceTicketAttachment;
DROP TABLE maintenanceLog;
DROP TABLE maintenanceInformation;
DROP TABLE maintenanceSchedule;
DROP TABLE maintenanceStaff;
DROP TABLE seats;
DROP TABLE renting_Spot;
DROP TABLE redemption_log;
DROP TABLE discount;
DROP TABLE SponsorshipRecord;
DROP TABLE merchant;
DROP TABLE member;
DROP TABLE admin;
DROP VIEW V_RentDetails
--============BUILD TABLE==============
--===========奕穎  ver 2026/1/30 更新會員欄位 ==============
/** =========================================================
   1) 建立 member & admin
   ========================================================= */
CREATE TABLE member
(
    memId INT IDENTITY(1,1) PRIMARY KEY,
    --會員ID
    memUsername VARCHAR(50) NOT NULL UNIQUE,
    --帳號
    memPassword VARCHAR(100) NOT NULL,
    --密碼
    memName NVARCHAR(50) NOT NULL,
    --姓名
    memEmail VARCHAR(50) NOT NULL UNIQUE,
    --信箱
    memPhone VARCHAR(20) NOT NULL,
    --手機號碼
    memStatus INT NOT NULL DEFAULT 1,
    --會員狀態
    createdAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    --建立時間
    updatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    --更新時間
    memPoints INT NOT NULL DEFAULT 0,
    --點數累計
    memViolation INT NOT NULL DEFAULT 0,
    --違規紀錄
    memLevel INT NOT NULL DEFAULT 1,
    --會員等級
    memInvoice VARCHAR(20) NULL
    --發票載具
);

--===========新增會員照片欄位 ==============
USE SeatRentSys;

ALTER TABLE member 
ADD memImage VARCHAR(255) CONSTRAINT DF_member_memImage DEFAULT 'default.png';


UPDATE member SET memImage = '01.jpg' WHERE memId = 1;
UPDATE member SET memImage = '02.jpg' WHERE memId = 2;
UPDATE member SET memImage = '03.jpg' WHERE memId = 3;
UPDATE member SET memImage = '04.jpg' WHERE memId = 4;
UPDATE member SET memImage = '05.jpg' WHERE memId = 5;
UPDATE member SET memImage = '06.jpg' WHERE memId = 6;
UPDATE member SET memImage = '07.jpg' WHERE memId = 7;
UPDATE member SET memImage = '08.jpg' WHERE memId = 8;
UPDATE member SET memImage = '09.jpg' WHERE memId = 9;
UPDATE member SET memImage = '10.jpg' WHERE memId = 10;


CREATE TABLE admin
(
    admId INT IDENTITY(1,1) PRIMARY KEY,
    --管理員ID
    admUsername VARCHAR(50) NOT NULL UNIQUE,
    --帳號
    admPassword VARCHAR(100) NOT NULL,
    --密碼
    admName NVARCHAR(50) NOT NULL,
    --姓名
    admEmail VARCHAR(50) NOT NULL,
    --信箱
    admRole INT NOT NULL DEFAULT 1,
    --管理員權限
    createdAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    --建立時間
    updatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    --更新時間
    admStatus TINYINT NOT NULL DEFAULT 1
    --管理員狀態
);
--========================== 翊庭 TABLE ver.20260121===================
CREATE TABLE merchant
(
    merchantId INT IDENTITY(1,1) PRIMARY KEY,
    -- PK
    merchantName NVARCHAR(50) NOT NULL,
    -- 商家名稱
    merchantPhone NVARCHAR(20),
    -- 電話
    merchantEmail NVARCHAR(50) NULL,
    -- Email
    merchantAddress NVARCHAR(200) NOT NULL,
    -- 地址
    merchantStatus INT,
    -- 狀態
    createdTime DATETIME2 DEFAULT SYSDATETIME()
    -- 建立時間
);

-- 建立外鍵約束：確保 renting_Spot 的 merchantId 必須存在於 merchant 表中 (或為 NULL)
-- 建立 renting_Spot 時，merchant 表格還不存在，所以必須使用 ALTER TABLE（事後補建）先建立表格，之後再加上外鍵約束
--ALTER TABLE renting_Spot ADD CONSTRAINT FK_renting_Spot_merchant FOREIGN KEY (merchantId) REFERENCES merchant(merchantId);

CREATE TABLE discount
(
    couponId INT IDENTITY(1,1) PRIMARY KEY,
    -- PK 優惠券ID
    couponName NVARCHAR(500),
    --優惠券名稱 
    couponDescription NVARCHAR(1000),
    -- 優惠內容
    pointsRequired INT NOT NULL,
    -- 點數需求
    startDate DATE,
    -- 生效日期
    endDate DATE,
    -- 結束日期
    merchantId INT,
    -- FK 商家ID
    couponStatus INT NOT NULL,
    -- 上下架
    createdTime DATETIME2 DEFAULT SYSDATETIME(),
    -- 建立時間
    couponImg NVARCHAR(500),
    -- 照片路徑 
    CONSTRAINT FK_discount_merchant
        FOREIGN KEY
(merchantId)
        REFERENCES merchant
(merchantId)
);

CREATE TABLE redemption_log
(
    logId INT IDENTITY(1,1) PRIMARY KEY,
    -- 自動遞增主鍵
    memId INT NOT NULL,
    -- 會員ID
    couponId INT NOT NULL,
    -- 優惠券ID
    pointsSpent INT NOT NULL,
    -- 當時扣除的點數
    couponName NVARCHAR(100),
    -- 優惠券名稱快照
    redeemTime DATETIME2 DEFAULT GETDATE()
    -- 核銷時間，預設為當前時間
);

-- 建議加上索引，未來資料量大時查詢較快
CREATE INDEX idx_redemption_memId ON redemption_log(memId);
CREATE INDEX idx_redemption_time ON redemption_log(redeemTime);

CREATE TABLE SponsorshipRecord
(
    SponsorID INT IDENTITY(1,1) PRIMARY KEY,
    MemberID INT NOT NULL,
    -- 關聯會員
    MerchantTradeNo NVARCHAR(50) NOT NULL,
    -- 傳給綠界的唯一訂單號
    TradeNo NVARCHAR(50),
    -- 綠界內部的交易序號
    Amount DECIMAL(10) NOT NULL,
    -- 贊助金額
    SponsorComment NVARCHAR(500),
    -- 贊助者的留言 (新需求)
    Status INT DEFAULT 0,
    -- 0:待處理, 1:成功, 2:失敗
    PaymentType NVARCHAR(20),
    -- 支付方式
    CreatedAt DATETIME DEFAULT GETDATE(),
    UpdatedAt DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Sponsorship_Member FOREIGN KEY (MemberID) REFERENCES Member(memID),
    CONSTRAINT UC_MerchantTradeNo UNIQUE (MerchantTradeNo)
);
--========================== 翊庭 TABLE END===================
--========光宇 Table ver.20240121  14:52========
/* =========================================================
   1) 建立 renting_Spot
   ========================================================= */
CREATE TABLE dbo.renting_Spot
(
    spotId INT IDENTITY(1,1),
    spotCode VARCHAR(30) NOT NULL,
    spotName NVARCHAR(100) NOT NULL,
    spotAddress NVARCHAR(200) NULL,
    spotStatus NVARCHAR(20) NOT NULL
        CONSTRAINT DF_renting_Spot_spotStatus DEFAULT (N'營運中'),
    merchantId INT NULL,
    createdAt DATETIME2(7) NOT NULL
        CONSTRAINT DF_renting_Spot_createdAt DEFAULT (SYSDATETIME()),
    updatedAt DATETIME2(7) NOT NULL
        CONSTRAINT DF_renting_Spot_updatedAt DEFAULT (SYSDATETIME()),
    latitude DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    spotDescription NVARCHAR(500) NULL,
    spotImage VARCHAR(255) NULL,
    CONSTRAINT PK_renting_Spot PRIMARY KEY CLUSTERED (spotId),
    CONSTRAINT CK_renting_Spot_spotStatus  CHECK (spotStatus IN (N'營運中', N'停用', N'維修中')),
    CONSTRAINT CK_renting_Spot_latlng_pair CHECK (
    (latitude IS NULL AND longitude IS NULL)
        OR
        (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
)
);
GO

CREATE UNIQUE NONCLUSTERED INDEX UQ_renting_Spot_spotCode ON dbo.renting_Spot (spotCode);
GO

CREATE TABLE dbo.seats
(
    seatsId INT IDENTITY(1,1),
    seatsName NVARCHAR(100) NOT NULL,
    seatsType NVARCHAR(50) NOT NULL,
    seatsStatus NVARCHAR(20) NOT NULL
        CONSTRAINT DF_seats_seatsStatus DEFAULT (N'啟用'),
    spotId INT NULL,
    updatedAt DATETIME2(7) NOT NULL
        CONSTRAINT DF_seats_updatedAt DEFAULT (SYSDATETIME()),
    -- serialNumber VARCHAR(50) NULL,
    serialNumber AS ('SN-' + CAST(YEAR(createdAt) AS VARCHAR(4)) + RIGHT('000000' + CAST(seatsId AS VARCHAR(10)), 6)) PERSISTED,
    createdAt DATETIME2(7) NOT NULL
        CONSTRAINT DF_seats_createdAt DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_seats PRIMARY KEY CLUSTERED (seatsId),
    CONSTRAINT FK_seats_spot
        FOREIGN KEY (spotId)
        REFERENCES dbo.renting_Spot (spotId),
    CONSTRAINT CK_seats_seatsStatus
        CHECK (seatsStatus IN (N'啟用', N'停用', N'維修中'))
);
GO

/* 一般索引：加速以 spotId 查 seats */
CREATE NONCLUSTERED INDEX IX_seats_spotId
ON dbo.seats (spotId);
GO

/* serialNumber 唯一索引 */
CREATE UNIQUE NONCLUSTERED INDEX UQ_seats_serialNumber
ON dbo.seats (serialNumber);
GO

/* =========================================================
   3) updatedAt 自動刷新 Trigger（AFTER UPDATE）
   ========================================================= */
CREATE TRIGGER dbo.trg_renting_Spot_updatedAt
ON dbo.renting_Spot
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE s
    SET updatedAt = SYSDATETIME()
    FROM dbo.renting_Spot s
        INNER JOIN inserted i ON s.spotId = i.spotId;
END;
GO

CREATE TRIGGER dbo.trg_seats_updatedAt
ON dbo.seats
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE s
    SET updatedAt = SYSDATETIME()
    FROM dbo.seats s
        INNER JOIN inserted i ON s.seatsId = i.seatsId;
END;
GO
--==============光宇 TABLE END==============

--==============翌帆 TABLE ver.20260121==============
USE [SeatRentSys];
GO

CREATE TABLE [dbo].[maintenanceStaff]
(
    [staffId] INT IDENTITY (1, 1) NOT NULL,
    [staffName] NVARCHAR (50) NOT NULL,
    [staffCompany] NVARCHAR (100) NULL,
    [staffPhone] VARCHAR (20) NULL,
    [staffEmail] VARCHAR (100) NULL,
    [staffNote] NVARCHAR (200) NULL,
    [createdAt] DATETIME2 (7) DEFAULT (sysdatetime()) NOT NULL,
    [isActive] BIT CONSTRAINT [DF_maintenanceStaff_isActive] DEFAULT ((1)) NOT NULL,
    PRIMARY KEY CLUSTERED ([staffId] ASC)
);
--===================================================
USE [SeatRentSys];
GO

CREATE TABLE [dbo].[maintenanceInformation]
(
    [ticketId] INT IDENTITY (1, 1) NOT NULL,
    [spotId] INT NULL,
    [issueType] NVARCHAR (200) NOT NULL,
    [issueDesc] NVARCHAR (500) NULL,
    [issuePriority] VARCHAR (100) DEFAULT ('NORMAL') NOT NULL,
    [issueStatus] VARCHAR (50) DEFAULT ('REPORTED') NOT NULL,
    [assignedStaffId] INT NULL,
    [reportedAt] DATETIME2 (7) DEFAULT (sysdatetime()) NOT NULL,
    [startAt] DATETIME2 (7) NULL,
    [resolvedAt] DATETIME2 (7) NULL,
    [resolveNote] NVARCHAR (500) NULL,
    [resultType] NVARCHAR (50) NULL,
    [seatsId] INT NULL,
    PRIMARY KEY CLUSTERED ([ticketId] ASC),
    CONSTRAINT [FK_maintenanceInformation_seats] FOREIGN KEY ([seatsId]) REFERENCES [dbo].[seats] ([seatsId]),
    CONSTRAINT [fkMaintAssignedStaffId] FOREIGN KEY ([assignedStaffId]) REFERENCES [dbo].[maintenanceStaff] ([staffId])
);
--=================================
--=========2026-1-31(翌帆新建)============

--========克服回報專用=================
USE [SeatRentSys];
GO

CREATE TABLE [dbo].[maintenanceTicketAttachment]
(
    [attachmentId] INT IDENTITY (1, 1) NOT NULL,
    [ticketId] INT NOT NULL,

    [originalName] NVARCHAR (255) NOT NULL,
    [storedName] NVARCHAR (255) NOT NULL,
    [contentType] NVARCHAR (100) NOT NULL,
    [fileSize] BIGINT NOT NULL,

    [publicUrl] NVARCHAR (400) NOT NULL,

    [sortOrder] INT DEFAULT ((0)) NOT NULL,
    [note] NVARCHAR (200) NULL,

    [createdAt] DATETIME2 (7) DEFAULT (sysdatetime()) NOT NULL,
    [isActive] BIT CONSTRAINT [DF_mta_isActive] DEFAULT ((1)) NOT NULL,

    CONSTRAINT [PK_maintenanceTicketAttachment] PRIMARY KEY CLUSTERED ([attachmentId] ASC),

    CONSTRAINT [FK_mta_ticket]
        FOREIGN KEY ([ticketId])
        REFERENCES [dbo].[maintenanceInformation] ([ticketId])
        ON DELETE CASCADE
);
GO

-- 常用查詢：用 ticketId 撈附件、按 createdAt 或 sortOrder 排序
CREATE NONCLUSTERED INDEX [IX_mta_ticket_createdAt]
ON [dbo].[maintenanceTicketAttachment] ([ticketId] ASC, [createdAt] DESC)
INCLUDE ([publicUrl], [isActive], [sortOrder]);
GO

-- 防止同一張工單重複插入同名 storedName（可選但很保險）
CREATE UNIQUE NONCLUSTERED INDEX [UX_mta_ticket_storedName]
ON [dbo].[maintenanceTicketAttachment] ([ticketId] ASC, [storedName] ASC);
GO

--=================================
USE [SeatRentSys];
GO
-- 建立排程表
CREATE TABLE [dbo].[maintenanceSchedule]
(
    [scheduleId] INT IDENTITY (1, 1) NOT NULL,
    [title] NVARCHAR (100) NOT NULL,
    -- 任務名稱    -- 目標設定 (Polymorphic Association)
    [targetType] VARCHAR (20) NOT NULL,
    -- 'SPOT' 或 'SEAT'
    [targetId] INT NOT NULL,
    -- 對應 renting_Spot.spotId 或 seats.seatsId
    -- 頻率設定
    [scheduleType] VARCHAR (20) NOT NULL,
    -- 'DAILY', 'WEEKLY', 'MONTHLY'
    [dayOfWeek] INT NULL,
    -- 1-7 (週排程用)
    [dayOfMonth] INT NULL,
    -- 1-31 (月排程用)
    [executeTime] TIME (7) NOT NULL,
    -- 預計執行時間
    -- 工單內容預設值
    [issueType] NVARCHAR (200) NOT NULL,
    [issuePriority] VARCHAR (50) DEFAULT ('NORMAL') NOT NULL,
    [assignedStaffId] INT NULL,
    -- 預設指派的虛擬廠商ID
    -- 排程控制
    [isActive] BIT DEFAULT ((1)) NOT NULL,
    [lastExecutedAt] DATETIME2 (7) NULL,
    [nextExecuteAt] DATETIME2 (7) NOT NULL,
    -- 系統自動計算的下次執行時間
    [createdAt] DATETIME2 (7) DEFAULT (sysdatetime()) NOT NULL,
    [updatedAt] DATETIME2 (7) DEFAULT (sysdatetime()) NOT NULL,
    PRIMARY KEY CLUSTERED ([scheduleId] ASC)
);
GO
-- =============================================
-- 以下是AI建議的強力約束 (Constraints)
-- =============================================

-- 1. 限制 targetType 只能是 SPOT 或 SEAT
ALTER TABLE [dbo].[maintenanceSchedule]
ADD CONSTRAINT [CK_schedule_targetType] 
CHECK ([targetType] IN ('SPOT', 'SEAT'));
-- 2. 限制 scheduleType 只能是三種頻率之一
ALTER TABLE [dbo].[maintenanceSchedule]
ADD CONSTRAINT [CK_schedule_scheduleType] 
CHECK ([scheduleType] IN ('DAILY', 'WEEKLY', 'MONTHLY'));
-- 3. 限制 issuePriority 只能是定義好的優先級
ALTER TABLE [dbo].[maintenanceSchedule]
ADD CONSTRAINT [CK_schedule_priority] 
CHECK ([issuePriority] IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'));
-- 4. 邏輯檢查：確保頻率與參數一致 (防呆)
-- DAILY: 不需要 dayOfWeek 和 dayOfMonth
-- WEEKLY: 必須有 dayOfWeek (1-7)
-- MONTHLY: 必須有 dayOfMonth (1-31)
ALTER TABLE [dbo].[maintenanceSchedule]
ADD CONSTRAINT [CK_schedule_rule_logic]
CHECK (
    ([scheduleType]='DAILY'   AND [dayOfWeek] IS NULL AND [dayOfMonth] IS NULL)
 OR ([scheduleType]='WEEKLY'  AND [dayOfWeek] BETWEEN 1 AND 7 AND [dayOfMonth] IS NULL)
 OR ([scheduleType]='MONTHLY' AND [dayOfMonth] BETWEEN 1 AND 31 AND [dayOfWeek] IS NULL)
);

-- 5. 建立 Foreign Key (只針對 assignedStaffId，因為 targetId 是動態的無法設 FK)
ALTER TABLE [dbo].[maintenanceSchedule]
ADD CONSTRAINT [FK_schedule_staff]
FOREIGN KEY ([assignedStaffId]) REFERENCES [dbo].[maintenanceStaff] ([staffId]);

-- 6. 建立索引 (Index) - 讓排程掃描速度飛快
CREATE NONCLUSTERED INDEX [IX_schedule_due_check]
ON [dbo].[maintenanceSchedule] ([isActive], [nextExecuteAt])
INCLUDE ([scheduleId]); -- 包含 ID 以加速查詢
GO

-- =============================================
-- 表名：maintenanceLog (維修歷程記錄表)
-- =============================================
USE [SeatRentSys];
GO
-- 1. 建立資料表 (如果表不存在才建立)
IF OBJECT_ID(N'dbo.maintenanceLog', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[maintenanceLog]
    (
        [logId] INT IDENTITY (1, 1) NOT NULL,
        -- 流水號 PK
        [ticketId] INT NOT NULL,
        -- 關聯原本的工單 ID
        [operator] NVARCHAR (50) NOT NULL,
        -- 操作者 (支援中文)
        [action] VARCHAR (50) NOT NULL,
        -- 動作代號 (英文)
        [comment] NVARCHAR (500) NULL,
        -- 詳細說明
        [createdAt] DATETIME2 (7) DEFAULT (sysdatetime()) NOT NULL,
        -- 發生時間 (預設當下)
        -- 設定主鍵 (Primary Key)
        CONSTRAINT [PK_maintenanceLog] PRIMARY KEY CLUSTERED ([logId] ASC)
    );
END;
    -- 2. 建立外鍵關聯 (如果 FK 不存在才建立)
    GO
IF NOT EXISTS (SELECT 1
FROM sys.foreign_keys
WHERE name = N'FK_maintenanceLog_ticket')
BEGIN
    ALTER TABLE [dbo].[maintenanceLog]
    ADD CONSTRAINT [FK_maintenanceLog_ticket] 
    FOREIGN KEY ([ticketId]) 
    REFERENCES [dbo].[maintenanceInformation] ([ticketId])
    ON DELETE CASCADE;
    -- 工單刪除時，歷史紀錄一併刪除
    PRINT ' 外鍵 FK_maintenanceLog_ticket 建立成功';
END;

-- 3. 建立查詢索引 (針對 Timeline 優化)
-- 這會讓 "SELECT * FROM logs WHERE ticketId = ? ORDER BY createdAt DESC" 飛快
IF NOT EXISTS (SELECT 1
FROM sys.indexes
WHERE name = N'IX_maintenanceLog_ticket_createdAt')
BEGIN
    CREATE NONCLUSTERED INDEX [IX_maintenanceLog_ticket_createdAt]
    ON [dbo].[maintenanceLog] ([ticketId] ASC, [createdAt] DESC);
    PRINT '效能索引 IX_maintenanceLog_ticket_createdAt 建立成功';
END;

--============子桓 TABLE ver.20260121============
CREATE TABLE recRent
(
    recSeqId INT IDENTITY(11730,1) NOT NULL,
    --  隱藏的流水號，負責自動遞增
    recId AS ('R' + RIGHT('00000' + CAST(recSeqId AS VARCHAR(5)), 5)) PERSISTED,-- 'R' + 補零至 9 位數
    memId INT NOT NULL,
    --  外鍵:Member ID (可為 NULL)
    couponId INT NULL,
    --  外鍵：使用的優惠方案
    seatsId VARCHAR(50) NOT NULL,
    --  外鍵：便攜座椅編號
    spotIdRent INT NOT NULL,
    --  外鍵：租用點機台 ID (必須與 spot 表格的 spotId 類型一致)
    spotIdReturn INT NULL,
    --  外鍵：歸還點機台 ID (可為 NULL)
    recRentDT2 DATETIME2(0) NOT NULL,
    -- 租用日期時間
    recReturnDT2 DATETIME2(0) NULL,
    -- 歸還日期時間 (未歸還時為 NULL)
    recUsageDT2 DECIMAL NULL,
    -- 使用時間 DATETIME2
    recStatus NVARCHAR(32) NULL,
    -- 訂單狀態
    recPrice INT NULL,
    -- 原始價格
    recRequestPay INT NULL,
    -- 請款金額
    recPayment INT NULL,
    -- 付款金額
    recPayBy VARCHAR(32) NULL,
    -- 付款方式
    recInvoice VARCHAR(16) NULL,
    -- 發票號碼
    recCarrier VARCHAR(16) NULL,
    -- 發票載具
    recViolatInt INT NOT NULL,
    -- 違規類型
    recNote NVARCHAR(255) NULL,
    -- 備註
    --Constraints
    CONSTRAINT PK_recRent_seq PRIMARY KEY (recSeqId),
    -- 技術主鍵
    CONSTRAINT UK_recRent_recId UNIQUE (recId),
    -- 業務主鍵 (R00001)
    --FK
    CONSTRAINT FK_recRent_MemberId FOREIGN KEY (memId) REFERENCES member(memId),
    CONSTRAINT FK_recRent_CouponId FOREIGN KEY (couponId) REFERENCES discount(couponId),
    CONSTRAINT FK_recRent_RentSpot FOREIGN KEY (spotIdRent) REFERENCES renting_Spot(spotId),
    CONSTRAINT FK_recRent_ReturnSpot FOREIGN KEY (spotIdReturn) REFERENCES renting_Spot(spotId)
);
--========================== 子桓 TABLE END===================
--===========BUILD TABLE END===============



--=========================TEST DATA===========================
--=================奕穎 DATA ver 20260210 =========================
INSERT INTO member
    (memUsername, memPassword, memName, memEmail, memPhone,
    memStatus, memPoints, memViolation, memLevel, memInvoice)
VALUES
    ('zzz', 'zzz', N'zzz', 'zzz@gmail.com', '0999888777', 1, 120, 0, 1, '/zzz9999'),
    ('alan123', 'pass123!', N'林小安', 'alan123@gmail.com', '0912345678', 1, 120, 0, 1, '/HH8U3V9'),
    ('betty5566', 'pass5566!', N'吳小美', 'betty5566@gmail.com', '0922666888', 1, 450, 1, 2, NULL),
    ('chung999', 'pass999!', N'張大中', 'chung999@gmail.com', '0933123456', 1, 80, 0, 1, '/ABCD123'),
    ('milktea07', 'tea2024!', N'陳奶茶', 'milk07@gmail.com', '0956123123', 1, 200, 2, 2, NULL),
    ('yoyo777', 'yo777###', N'黃悠悠', 'yoyo777@gmail.com', '0966333123', 1, 910, 0, 3, '/LL89KK2'),
    ('tony888', 't888pass', N'王東尼', 'tony888@gmail.com', '0911222333', 1, 50, 0, 1, NULL),
    ('qiqi520', 'q520love', N'蔡琪琪', 'qiqi520@gmail.com', '0988999555', 1, 375, 1, 2, '/QR88991'),
    ('jason007', 'j007pwd', N'李宗翰', 'jason007@gmail.com', '0988777666', 1, 135, 0, 1, NULL),
    ('jason008', 'j007pwd', N'李宗翰', 'jason008@gmail.com', '0988777666', 1, 135, 0, 1, NULL),
    ('jason009', 'j007pwd', N'李宗翰', 'jason009@gmail.com', '0988777666', 1, 135, 0, 1, NULL),
    ('jason0010', 'j007pwd', N'李宗翰', 'jason010@gmail.com', '0988777666', 1, 135, 0, 1, NULL),
    ('jason07', 'j007pwd', N'李宗翰', 'jaon007@gmail.com', '0988778666', 1, 135, 0, 1, NULL),
    ('apple321', 'a321pass', N'鄭雅萍', 'apple321@gmail.com', '0977555444', 1, 520, 2, 3, '/UUII77T');

INSERT INTO admin
    (admUsername, admPassword, admName, admEmail, admRole)
VALUES

    ('aaa', 'aaa', N'系統管理員', 'aaa@system.com', 9),
    ('admin001', 'Root001!', N'系統管理員', 'admin001@system.com', 9),
    ('staff01', 'Staff01@', N'王芳儀', 'staff01@system.com', 1),
    ('staff02', 'Staff02@', N'林建宏', 'staff02@system.com', 1),
    ('manager01', 'Mgr2024$', N'陳偉倫', 'manager01@system.com', 9),
    ('super88', 'Super88#', N'張芷晴', 'super88@system.com', 1),
    ('admin002', 'Pass1234', N'王大明', 'admin002@system.com', 1),
    ('sysadmin', 'Pass1234', N'林小華', 'sysadmin@system.com', 1),
    ('manager02', 'Pass1234', N'陳美麗', 'manager02@system.com', 9),
    ('backend01', 'Pass1234', N'張志宏', 'backend01@system.com', 1);

--=================更新資料成真實資料 ver 20260210 =========================
UPDATE Member SET memName = '陳冠廷', memPhone = '0912445892', createdAt = '2026-01-21 10:15:30' WHERE memId = 1;
UPDATE Member SET memName = '林志宏', memPhone = '0933612301', createdAt = '2026-01-21 16:45:05' WHERE memId = 2;
UPDATE Member SET memName = '李雅婷', memPhone = '0921554398', createdAt = '2026-01-22 14:22:11' WHERE memId = 3;
UPDATE Member SET memName = '黃婉婷', memPhone = '0910832654', createdAt = '2026-01-22 14:22:11' WHERE memId = 4;
UPDATE Member SET memName = '張家豪', memPhone = '0987154226', createdAt = '2026-01-23 09:30:44' WHERE memId = 5;
UPDATE Member SET memName = '吳佩珊', memPhone = '0975412987', createdAt = '2026-01-23 09:30:44' WHERE memId = 6;
UPDATE Member SET memName = '王柏翰', memPhone = '0952789413', createdAt = '2026-01-25 21:10:12' WHERE memId = 7;
UPDATE Member SET memName = '蔡依潔', memPhone = '0963201547', createdAt = '2026-01-25 21:10:12' WHERE memId = 8;
UPDATE Member SET memName = '楊曉菁', memPhone = '0918667312', createdAt = '2026-01-25 21:10:12' WHERE memId = 9;
UPDATE Member SET memName = '許銘傑', memPhone = '0905228765', createdAt = '2026-01-25 21:10:12' WHERE memId = 10;

UPDATE Admin SET admName = '林美惠', createdAt = '2026-01-01 09:00:00', admEmail = 'meihui@gmail.com' WHERE admId = 1;
UPDATE Admin SET admName = '陳淑芬', createdAt = '2026-01-01 13:30:45', admEmail = 'shufen@gmail.com' WHERE admId = 2;
UPDATE Admin SET admName = '張家銘', createdAt = '2026-01-03 08:50:12', admEmail = 'jiaming@gmail.com' WHERE admId = 3;
UPDATE Admin SET admName = '李佳玲', createdAt = '2026-01-05 10:15:30', admEmail = 'jialing@gmail.com' WHERE admId = 4;
UPDATE Admin SET admName = '王俊傑', createdAt = '2026-01-06 09:11:22', admEmail = 'alan123149@gmail.com' WHERE admId = 5;
UPDATE Admin SET admName = '吳佩芬', createdAt = '2026-01-08 16:40:05', admEmail = 'peifen@gmail.com' WHERE admId = 6;
UPDATE Admin SET admName = '黃雅婷', createdAt = '2026-01-10 08:20:55', admEmail = 'yating@gmail.com' WHERE admId = 7;
UPDATE Admin SET admName = '蔡宜君', createdAt = '2026-01-12 11:05:18', admEmail = 'yichun@gmail.com' WHERE admId = 8;
UPDATE Admin SET admName = '楊惠雯', createdAt = '2026-01-15 09:45:33', admEmail = 'huiwen@gmail.com' WHERE admId = 9;
UPDATE Admin SET admName = '許嘉玲', createdAt = '2026-01-18 14:12:00', admEmail = 'jialing.hsu@gmail.com' WHERE admId = 10;
--===============奕穎 DATA END=================

--================翊庭 DATA ver.20260121==============
INSERT INTO merchant
    (merchantName, merchantPhone, merchantEmail, merchantAddress, merchantStatus)
VALUES
    ('小王早餐店', N'0912-111-111', N'wang.breakfast@mail.com', N'台北市大安區和平東路100號', 1),
    ('天天飲料店', N'0922-222-222', N'drinkdaily@mail.com', N'台北市信義區光復南路88號', 1),
    ('阿中滷味', N'0933-333-333', N'achung@mail.com', N'新北市板橋區文化路50號', 1),
    ('幸福蛋糕店', N'0944-444-444', N'happycake@mail.com', N'台中市西屯區台灣大道200號', 1),
    ('老張牛肉麵', N'0955-555-555', N'beefnoodle@mail.com', N'高雄市苓雅區成功一路90號', 1),
    ('元氣便當店', N'0966-666-666', N'lunchbox@mail.com', N'台北市北投區中央北路300號', 1),
    ('慢活咖啡館', N'0977-777-777', N'slowcoffee@mail.com', N'台南市中西區民族路30號', 1),
    ('阿美水果行', N'0988-888-888', N'fruitamei@mail.com', N'桃園市中壢區中正路120號', 1),
    ('新味壽司', N'0999-999-999', N'newtaste@mail.com', N'新竹市東區光明路66號', 1),
    ('樂町火鍋', N'0910-010-010', N'hotpotjoy@mail.com', N'嘉義市西區文化路45號', 1);

INSERT INTO discount
    (couponName,couponDescription, pointsRequired, startDate, endDate, merchantId, couponStatus,couponImg)
VALUES
    ('滿100折10', '滿100折10', 50, '2025-06-01', '2026-12-31', 1, 1, '滿100折10元.jpg'),
    ('第二杯半價', '第二杯半價', 80, '2025-06-15', '2026-06-30', 2, 1, '第二杯半價.jpg'),
    ('滷味滿200折30', '滷味滿200折30', 60, '2025-02-01', '2026-12-31', 3, 1, '滿200折20.jpg'),
    ('蛋糕買一送一（限9吋）', '蛋糕買一送一（限9吋）', 200, '2026-03-01', '2025-04-30', 4, 1, '買1送1.jpg'),
    ('牛肉麵免費加麵', '牛肉麵免費加麵', 40, '2025-06-01', '2026-12-31', 5, 1, '免費續湯加麵.jpg'),
    ('便當加菜優惠折20', '便當加菜優惠折20', 30, '2025-02-10', '2026-12-31', 6, 1, '20元折扣.jpg'),
    ('咖啡任選飲品折15', '咖啡任選飲品折15', 70, '2025-06-20', '2026-09-30', 7, 1, '15元折扣.jpg'),
    ('水果禮盒9折', '水果禮盒9折', 150, '2025-04-01', '2026-12-31', 8, 1, '9折.jpg'),
    ('壽司套餐折50', '壽司套餐折50', 120, '2025-06-01', '2026-12-31', 9, 1, '折50.jpg'),
    ('火鍋套餐滿500折100', '火鍋套餐滿500折100', 180, '2025-02-15', '2026-12-31', 10, 1, '500元套餐折價100.jpg');
--=========================================翊庭 TEST DATA  END===================================
--=======================光宇DATA ver1150206- 1150121===================
-- 桃園市區域測試資料 (20筆)- 不插入 spotStatus，改吃 DEFAULT (N'營運中')
INSERT INTO dbo.renting_Spot
    (spotCode, spotName, spotAddress, merchantId, latitude, longitude)
VALUES
    (N'TYN001', N'桃園高鐵站', N'桃園市中壢區高鐵北路一段6號', NULL, 25.0133300, 121.2144300),
    (N'TYN002', N'中壢火車站', N'桃園市中壢區中和路139號', 1, 24.9537811, 121.2255764),
    (N'TYN003', N'桃園火車站', N'桃園市桃園區萬壽路三段123號', 102, 24.9898236, 121.3134382),
    (N'TYN004', N'內壢火車站', N'桃園市中壢區中華路一段27號', NULL, 24.9748600, 121.2673200),
    (N'TYN005', N'埔心火車站', N'桃園市楊梅區永美路2號', NULL, 24.9284000, 121.1810500),
    (N'TYN006', N'桃園市政府', N'桃園市桃園區縣府路1號', NULL, 24.9930115, 121.3015976),
    (N'TYN007', N'桃園展演中心', N'桃園市桃園區中正路1188號', 105, 25.0152016, 121.3009949),
    (N'TYN008', N'國立中央大學', N'桃園市中壢區中大路300號', NULL, 24.9682000, 121.1921200),
    (N'TYN009', N'元智大學', N'桃園市中壢區遠東路135號', NULL, 24.9715100, 121.2690300),
    (N'TYN010', N'長庚大學', N'桃園市龜山區文化一路259號', NULL, 25.0441500, 121.3857600),
    (N'TYN011', N'台茂購物中心', N'桃園市蘆竹區南崁路一段112號', 108, 25.0475311, 121.2926712),
    (N'TYN012', N'大江國際購物中心', N'桃園市中壢區中園路二段501號', 109, 25.0130692, 121.2335663),
    (N'TYN013', N'華泰名品城', N'桃園市中壢區春德路189號', 110, 25.0425301, 121.2148132),
    (N'TYN014', N'統領廣場', N'桃園市桃園區中正路61號', 1, 24.9902600, 121.3138800),
    (N'TYN015', N'中原夜市', N'桃園市中壢區實踐路', NULL, 24.9575900, 121.2393300),
    (N'TYN016', N'竹圍漁港', N'桃園市大園區沙崙里1鄰10號', NULL, 25.1118100, 121.2096300),
    (N'TYN017', N'石門水庫', N'桃園市大溪區復興里環湖路一段68號', NULL, 24.8118000, 121.2464000),
    (N'TYN018', N'小人國主題樂園', N'桃園市龍潭區高原路891號', NULL, 24.8315100, 121.1895600),
    (N'TYN019', N'桃園國際棒球場', N'桃園市中壢區領航北路一段1號', NULL, 25.0345000, 121.2036000),
    (N'TYN020', N'林口長庚紀念醫院', N'桃園市龜山區復興街5號', NULL, 25.0494400, 121.3713900);


-- 台北市區域測試資料 (20筆) - 不插入 spotStatus，改吃 DEFAULT (N'營運中')
INSERT INTO dbo.renting_Spot
    (spotCode, spotName, spotAddress, merchantId, latitude, longitude)
VALUES
    (N'TPE001', N'台北101', N'台北市信義區信義路五段7號', 1, 25.0339640, 121.5644720),
    (N'TPE002', N'故宮博物院', N'台北市士林區至善路二段221號', NULL, 25.1022200, 121.5484200),
    (N'TPE003', N'台北車站', N'台北市中正區黎明里北平西路3號', 202, 25.0477600, 121.5170900),
    (N'TPE004', N'西門紅樓', N'台北市萬華區成都路10號', NULL, 25.0423000, 121.5073600),
    (N'TPE005', N'總統府', N'台北市中正區重慶南路一段122號', NULL, 25.0403000, 121.5117200),
    (N'TPE006', N'中正紀念堂', N'台北市中正區中山南路21號', NULL, 25.0348200, 121.5219200),
    (N'TPE007', N'國立臺灣大學', N'台北市大安區羅斯福路四段1號', NULL, 25.0173500, 121.5397500),
    (N'TPE008', N'台北市立動物園', N'台北市文山區新光路二段30號', NULL, 24.9984900, 121.5810700),
    (N'TPE009', N'松山文創園區', N'台北市信義區光復南路133號', 205, 25.0436900, 121.5601500),
    (N'TPE010', N'士林夜市', N'台北市士林區基河路101號', NULL, 25.0877400, 121.5242400),
    (N'TPE011', N'台北市政府', N'台北市信義區市府路1號', NULL, 25.0375000, 121.5636100),
    (N'TPE012', N'SOGO忠孝館', N'台北市大安區忠孝東路四段45號', 208, 25.0430200, 121.5445200),
    (N'TPE014', N'美麗華百樂園', N'台北市中山區敬業三路20號', 210, 25.0833900, 121.5562700),
    (N'TPE015', N'台北小巨蛋', N'台北市松山區南京東路四段2號', NULL, 25.0514100, 121.5531700),
    (N'TPE017', N'捷運市政府站', N'台北市信義區忠孝東路五段6號', NULL, 25.0409000, 121.5645000),
    (N'TPE018', N'捷運中山站', N'台北市大同區南京西路16號', NULL, 25.0524400, 121.5204400),
    (N'TPE019', N'捷運東門站', N'台北市中正區信義路二段166號', NULL, 25.0330600, 121.5292400),
    (N'TPE020', N'南港展覽館1館', N'台北市南港區經貿二路1號', NULL, 25.0551800, 121.6154600);

-- 台中市區域測試資料 (25筆)
INSERT INTO dbo.renting_Spot
    (spotCode, spotName, spotAddress, merchantId, latitude, longitude)
VALUES
    (N'TCH001', N'台中火車站', N'台中市中區台灣大道一段1號', 1, 24.136829, 120.685011),
    (N'TCH002', N'高鐵台中站', N'台中市烏日區站區二路8號', NULL, 24.112074, 120.615684),
    (N'TCH003', N'逢甲夜市', N'台中市西屯區文華路', NULL, 24.178982, 120.646634),
    (N'TCH004', N'臺中國家歌劇院', N'台中市西屯區惠來路二段101號', NULL, 24.162863, 120.640428),
    (N'TCH005', N'自然科學博物館', N'台中市北區館前路1號', NULL, 24.157273, 120.666038),
    (N'TCH006', N'勤美誠品綠園道', N'台中市西區公益路68號', 302, 24.151125, 120.663823),
    (N'TCH007', N'新光三越中港店', N'台中市西屯區台灣大道三段301號', 303, 24.164554, 120.643393),
    (N'TCH008', N'台中大遠百', N'台中市西屯區台灣大道三段251號', 304, 24.164926, 120.644538),
    (N'TCH009', N'宮原眼科', N'台中市中區中山路20號', 5, 24.137778, 120.683459),
    (N'TCH010', N'審計新村', N'台中市西區民生路368巷', NULL, 24.144257, 120.662758),
    (N'TCH011', N'一中商圈', N'台中市北區一中街', NULL, 24.149238, 120.685290),
    (N'TCH012', N'台中公園', N'台中市北區雙十路一段65號', NULL, 24.145196, 120.683758),
    (N'TCH013', N'東海大學', N'台中市西屯區台灣大道四段1727號', NULL, 24.181583, 120.603125),
    (N'TCH014', N'彩虹眷村', N'台中市南屯區春安路56巷25號', NULL, 24.133914, 120.609838),
    (N'TCH015', N'LaLaport 台中', N'台中市東區進德路700號', 3, 24.133500, 120.693000),
    (N'TCH016', N'臺中市政府', N'台中市西屯區台灣大道三段99號', NULL, 24.161804, 120.646880),
    (N'TCH017', N'文心森林公園', N'台中市南屯區文心路一段289號', NULL, 24.145556, 120.645556),
    (N'TCH018', N'豐原火車站', N'台中市豐原區中正路1號', NULL, 24.254072, 120.722900),
    (N'TCH019', N'大慶火車站', N'台中市南區建國北路一段11號', NULL, 24.115147, 120.654333),
    (N'TCH020', N'秋紅谷公園', N'台中市西屯區朝富路30號', NULL, 24.167583, 120.639528),
    (N'TCH021', N'台灣美術館', N'台中市西區五權西路一段2號', NULL, 24.141211, 120.663119),
    (N'TCH022', N'草悟道', N'台中市西區向上北路', NULL, 24.148889, 120.664167),
    (N'TCH023', N'廣三SOGO百貨', N'台中市西區台灣大道二段459號', 307, 24.155639, 120.663306),
    (N'TCH024', N'中友百貨', N'台中市北區三民路三段161號', 308, 24.152333, 120.684667),
    (N'TCH025', N'麗寶樂園', N'台中市后里區福容路8號', NULL, 24.323056, 120.693611);

-- 高雄市區域測試資料 (25筆)
INSERT INTO dbo.renting_Spot
    (spotCode, spotName, spotAddress, merchantId, latitude, longitude)
VALUES
    (N'KHH001', N'高雄火車站', N'高雄市三民區建國二路318號', 1, 22.639761, 120.302453),
    (N'KHH002', N'高鐵左營站', N'高雄市左營區高鐵路105號', NULL, 22.687353, 120.307872),
    (N'KHH003', N'駁二藝術特區', N'高雄市鹽埕區大勇路1號', NULL, 22.620417, 120.281722),
    (N'KHH004', N'高雄85大樓', N'高雄市苓雅區自強三路5號', NULL, 22.611722, 120.300333),
    (N'KHH005', N'統一夢時代購物中心', N'高雄市前鎮區中華五路789號', 402, 22.595139, 120.306944),
    (N'KHH006', N'漢神巨蛋購物廣場', N'高雄市左營區博愛二路777號', 403, 22.669583, 120.302361),
    (N'KHH007', N'瑞豐夜市', N'高雄市左營區裕誠路', NULL, 22.665833, 120.299444),
    (N'KHH008', N'六合夜市', N'高雄市新興區六合二路', NULL, 22.633056, 120.301389),
    (N'KHH009', N'旗津輪渡站', N'高雄市旗津區海岸路10號', NULL, 22.613889, 120.270278),
    (N'KHH010', N'衛武營國家藝術文化中心', N'高雄市鳳山區三多一路1號', NULL, 22.623611, 120.341667),
    (N'KHH011', N'高雄市立美術館', N'高雄市鼓山區美術館路80號', NULL, 22.658056, 120.286111),
    (N'KHH012', N'蓮池潭', N'高雄市左營區蓮潭路', NULL, 22.683333, 120.296667),
    (N'KHH013', N'愛河之心', N'高雄市三民區博愛一路', NULL, 22.653333, 120.303333),
    (N'KHH014', N'中央公園', N'高雄市前金區中山一路11號', NULL, 22.625000, 120.300000),
    (N'KHH015', N'SKM Park Outlets 高雄草衙', N'高雄市前鎮區中安路1-1號', 404, 22.581111, 120.329444),
    (N'KHH016', N'高雄市立圖書館總館', N'高雄市前鎮區新光路61號', NULL, 22.610278, 120.301667),
    (N'KHH017', N'西子灣風景區', N'高雄市鼓山區蓮海路70號', NULL, 22.626667, 120.265833),
    (N'KHH018', N'打狗英國領事館', N'高雄市鼓山區蓮海路20號', NULL, 22.618611, 120.267222),
    (N'KHH019', N'新堀江商圈', N'高雄市新興區文橫二路', NULL, 22.623056, 120.303889),
    (N'KHH020', N'捷運美麗島站', N'高雄市新興區中山一路115號', NULL, 22.631389, 120.301944),
    (N'KHH021', N'義大世界', N'高雄市大樹區學城路一段10號', 5, 22.730278, 120.409722),
    (N'KHH022', N'棧貳庫KW2', N'高雄市鼓山區蓬萊路17號', 6, 22.619167, 120.278611),
    (N'KHH023', N'高雄流行音樂中心', N'高雄市鹽埕區真愛路1號', NULL, 22.619444, 120.286667),
    (N'KHH024', N'壽山動物園', N'高雄市鼓山區萬壽路350號', NULL, 22.633333, 120.275000),
    (N'KHH025', N'國立科學工藝博物館', N'高雄市三民區九如一路720號', NULL, 22.640833, 120.322500);

-- seats（不插入 seatsStatus，改吃 DEFAULT(N'啟用')）

-- [新增] 自動生成 300 筆座椅測試資料
DECLARE @i INT = 1;
DECLARE @seatType NVARCHAR(50);
DECLARE @seatTypeName NVARCHAR(100);

WHILE @i <= 300
BEGIN
    -- 先決定隨機類型
    SET @seatType = CASE ABS(CHECKSUM(NEWID())) % 3 WHEN 0 THEN N'B椅' WHEN 1 THEN N'E椅' ELSE N'S椅' END;
    SET @seatTypeName = CASE @seatType WHEN N'B椅' THEN N'基本椅' WHEN N'E椅' THEN N'加強椅' ELSE N'置物椅' END;

    INSERT INTO dbo.seats
        (seatsName, seatsType, spotId, updatedAt,createdAt)
    VALUES
        (
            @seatTypeName + N'-' + RIGHT('000' + CAST(@i AS VARCHAR(3)), 3), -- 第一欄: 名稱與類型對應，且不重複
            @seatType, -- 第二欄: 根據上面決定的類型填入
            (ABS(CHECKSUM(NEWID())) % 45) + 1, -- 第三欄: spotId 1~45 隨機分佈
            SYSDATETIME(),
            --RIGHT('000' + CAST(@i + 2 AS VARCHAR(3)), 3), -- 第五欄: 序號遞增 (接續 001, 002，從 003 開始)
            DEFAULT
    );
    SET @i = @i + 1;
END;
GO
--=========================================光宇 TEST DATA  END===================================
/* ============================================================
=========================================================
   SEATS 假資料
   - 不插 seatsStatus（吃 DEFAULT 啟用）
   - 不插 createdAt / updatedAt（吃 DEFAULT；UPDATE 才會 trigger）
   - 不插 serialNumber（computed persisted 自動算）
   - 不硬寫 spotId，改用 spotCode 找 spotId（可重跑、可移植）
========================================================= */

INSERT INTO dbo.seats
    (seatsName, seatsType, spotId, createdAt)
    SELECT N'置物椅-A01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN011'
UNION ALL
    SELECT N'置物椅-A02', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN011'
UNION ALL
    SELECT N'置物椅-A03', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN011'
UNION ALL
    SELECT N'置物椅-B01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN012'
UNION ALL
    SELECT N'置物椅-B02', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN012'
UNION ALL
    SELECT N'基本椅-C01', N'B椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN013'
UNION ALL
    SELECT N'基本椅-C02', N'B椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN013'
UNION ALL
    SELECT N'置物椅-D01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN014'
UNION ALL
    SELECT N'置物椅-D02', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN014'
UNION ALL
    SELECT N'基本椅-E01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN015'
UNION ALL
    SELECT N'基本椅-F01', N'B椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN016'
UNION ALL
    SELECT N'基本椅-G01', N'B椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN017'
UNION ALL
    SELECT N'置物椅-H01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN018'
UNION ALL
    SELECT N'置物椅-I01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN019'
UNION ALL
    SELECT N'置物椅-J01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN020'
UNION ALL
    SELECT N'置物椅-A01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN001'
UNION ALL
    SELECT N'置物椅-A02', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN001'
UNION ALL
    SELECT N'置物椅-A03', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN001'
UNION ALL
    SELECT N'置物椅-B01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN002'
UNION ALL
    SELECT N'置物椅-B02', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN002'
UNION ALL
    SELECT N'基本椅-C01', N'B椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN003'
UNION ALL
    SELECT N'基本椅-C02', N'B椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN003'
UNION ALL
    SELECT N'置物椅-D01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN004'
UNION ALL
    SELECT N'置物椅-D02', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN004'
UNION ALL
    SELECT N'基本椅-E01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN005'
UNION ALL
    SELECT N'基本椅-F01', N'B椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN006'
UNION ALL
    SELECT N'基本椅-G01', N'B椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN007'
UNION ALL
    SELECT N'置物椅-H01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN008'
UNION ALL
    SELECT N'置物椅-I01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN009'
UNION ALL
    SELECT N'置物椅-J01', N'E椅', spotId, '2026-01-01'
    FROM dbo.renting_Spot
    WHERE spotCode = 'TYN010';

-- 允許不綁 spot 的備用設備
INSERT INTO dbo.seats
    (seatsName, seatsType, spotId, createdAt)
VALUES
    (N'備用設備-Z99', N'S椅', NULL, '2026-01-01');

--=========================================光宇 TEST DATA  END===================================

USE [SeatRentSys];
INSERT INTO [dbo].[maintenanceStaff]
    ([staffName], [staffCompany], [staffPhone], [staffEmail], [staffNote], [isActive])
VALUES
    (N'陳國榮', N'永安機電工程行', '0912-345-678', 'kuorong.chen@yongan-fix.com', N'特約水電師傅，負責電力線路查修', 1),
    (N'林雅婷', N'潔淨家園服務社', '0922-123-456', 'yating.lin@cleanhome.tw', N'外包清潔廠商，負責場地日常打掃', 1),
    (N'黃志明', N'極速電腦工作室', '0933-987-654', 'cm.huang@speedy-pc.tw', N'負責機台硬體故障排除 (螢幕、主機)', 1),
    (N'張惠雯', NULL, '0911-222-333', 'huiwen.chang@gmail.com', N'個人接案清潔人員，配合彈性高', 1),
    (N'李建華', N'光速網路企業社', '0955-666-777', 'ch.lee@lightnet.com.tw', N'網路佈線與連線異常處理廠商', 1),
    (N'王怡君', N'安心監控科技', '0988-555-444', 'yichun.wang@safe-monitor.com', N'負責門禁系統與監視器設備維護', 1),
    (N'劉志偉', N'涼爽空調維修站', '0977-111-222', 'cw.liu@cool-ac.tw', N'負責空調設備保養與通風問題', 1),
    (N'吳淑芬', NULL, '0966-888-999', 'shufen.wu@yahoo.com.tw', N'臨時工，負責支援緊急清潔任務', 1),
    (N'蔡明哲', N'智匯資訊科技', '0921-000-111', 'mingche.tsai@smart-it.com.tw', N'軟體系統重灌與設定支援', 1),
    (N'楊宗翰', N'頂尖程式工作室', '0932-444-555', 'th.yang@top-code.com', N'遠端系統除錯與軟體更新協助', 1),
    (N'許家豪', N'強力電力工程', '0910-123-123', 'chiahao.hsu@power-fix.tw', N'高壓電設備檢修與配電盤維護', 1),
    (N'鄭淑惠', N'亮晶晶清潔公司', '0958-777-888', 'shuhui.cheng@shining.com', N'定期深度清潔與消毒作業', 1),
    (N'謝欣怡', N'訊號通訊行', '0917-555-666', 'hsinyi.hsieh@signal-comm.tw', N'無線網路訊號測試與優化', 1),
    (N'洪志強', N'阿強綜合水電', '0929-333-444', 'cc.hung@gmail.com', N'假日緊急叫修支援 (個人)', 1),
    (N'曾國華', N'順風家電維修', '0935-112-233', 'kh.tseng@wind-fix.com', N'一般電器設備更換與維修', 1),
    (N'廖俊傑', N'全能修繕工程', '0918-999-000', 'jj.liao@all-fix.com', N'桌椅結構損壞修補與更換', 1),
    (N'賴秀英', N'美好環境維護', '0920-555-123', 'hsiuying.lai@nice-env.com', N'負責垃圾清運與資源回收分類', 1),
    (N'徐文雄', N'金鑰匙鎖印行', '0970-111-999', 'wh.hsu@key-lock.tw', N'電子鎖電池更換與開鎖服務', 1),
    (N'蘇郁婷', N'連線通科技', '0916-222-888', 'yuting.su@connect-tech.com', N'路由器與交換器硬體設定', 1);


USE [SeatRentSys];
GO

INSERT INTO [dbo].[maintenanceInformation]
    (
    [spotId],
    [seatsId],
    [issueType],
    [issueDesc],
    [issuePriority],
    [issueStatus],
    [assignedStaffId],
    [startAt],
    [resolvedAt],
    [resolveNote],
    [resultType]
    )
VALUES
    -- 1 機台：螢幕閃爍
    (1, NULL, N'機台故障異常', N'機台螢幕閃爍且亮度不穩，疑似排線接觸不良，已重新固定並測試正常。', 'HIGH', 'RESOLVED', 1, DATEADD(HOUR,-6, SYSDATETIME()), DATEADD(HOUR,-3, SYSDATETIME()), N'已完成檢修，顯示正常。', N'REPAIRED'),

    -- 2 椅子：升降失效
    (1, 1, N'椅子損壞', N'椅子高度調整失效，已更換並確認可正常升降。', 'NORMAL', 'RESOLVED', 2, DATEADD(HOUR,-20,SYSDATETIME()), DATEADD(HOUR,-18,SYSDATETIME()), N'已更換零件，功能正常。', N'REPAIRED'),

    -- 3 機台：網路斷線
    (2, NULL, N'機台故障異常', N'機台網路間歇性斷線，已更換網路線並重啟設備，連線恢復穩定。', 'URGENT', 'RESOLVED', 5, DATEADD(DAY,-2, SYSDATETIME()), DATEADD(DAY,-2, DATEADD(HOUR,2,SYSDATETIME())), N'已排除斷線原因。', N'REPAIRED'),

    -- 4 椅子：鎖扣卡死
    (2, 5, N'椅子損壞', N'椅子無法解鎖，鎖扣機構卡死，已清潔調整並上潤滑，解鎖正常。', 'LOW', 'RESOLVED', 6, DATEADD(DAY,-3, SYSDATETIME()), DATEADD(DAY,-3, DATEADD(HOUR,1,SYSDATETIME())), N'已清潔保養，恢復正常。', N'REPAIRED'),

    -- 5 機台：噪音
    (3, NULL, N'機台故障異常', N'機台風扇噪音過大，已清潔灰塵並更換風扇，運轉噪音改善。', 'NORMAL', 'RESOLVED', 7, DATEADD(DAY,-4, SYSDATETIME()), DATEADD(DAY,-4, DATEADD(HOUR,3,SYSDATETIME())), N'已更換風扇，散熱正常。', N'REPAIRED'),

    -- 6 椅子：椅背晃動
    (3, 9, N'椅子損壞', N'椅背搖晃且有異音，已重新鎖固螺絲並加強固定，異音消失。', 'HIGH', 'RESOLVED', 16, DATEADD(DAY,-5, SYSDATETIME()), DATEADD(DAY,-5, DATEADD(HOUR,2,SYSDATETIME())), N'已鎖固加強，結構穩定。', N'REPAIRED'),

    -- 7 機台：網路斷線
    (4, NULL, N'機台故障異常', N'機台網路間歇性斷線，已更換網路線並重啟設備，連線恢復穩定。', 'HIGH', 'RESOLVED', 9, DATEADD(DAY,-6, SYSDATETIME()), DATEADD(DAY,-6, DATEADD(HOUR,1,SYSDATETIME())), N'已恢復刷卡功能。', N'REPAIRED'),

    -- 8 椅子：輪子卡住
    (4, 12, N'椅子損壞', N'椅子卡髒東西導致滑動困難，已清潔輪組並上潤滑，滑動順暢。', 'LOW', 'RESOLVED', 8, DATEADD(DAY,-7, SYSDATETIME()), DATEADD(DAY,-7, DATEADD(HOUR,1,SYSDATETIME())), N'已清潔輪組，狀態正常。', N'REPAIRED'),

    -- 9 機台：保養（清潔散熱）
    (5, NULL, N'保養', N'定期保養：清潔散熱孔與風扇區域，並進行溫度與運轉測試，結果正常。', 'NORMAL', 'RESOLVED', 3, DATEADD(DAY,-8, SYSDATETIME()), DATEADD(DAY,-8, DATEADD(HOUR,2,SYSDATETIME())), N'保養完成，運作正常。', N'MAINTAINED'),

    -- 10 椅子：感應異常（保養）
    (5, 20, N'保養', N'定期保養：椅面感應偶發異常，已重新校正感應器並測試占用狀態回報正常。', 'NORMAL', 'RESOLVED', 10, DATEADD(DAY,-9, SYSDATETIME()), DATEADD(DAY,-9, DATEADD(HOUR,2,SYSDATETIME())), N'校正完成，回報正常。', N'MAINTAINED');
GO

--=============================== 翌帆 DATA END======================================
--==============子桓 DATAver.20260121===============
INSERT INTO recRent
    (memId, couponId, seatsId, spotIdRent, spotIdReturn, recRentDT2, recReturnDT2, recUsageDT2, recStatus,
    recPrice, recRequestPay, recPayment, recPayBy, recInvoice, recCarrier, recViolatInt, recNote)
VALUES
    -- 1~5: 已完成的訂單 (同點歸還)
    (1, NULL, '001', 1, 9, '2023-03-12 10:00:00', '2023-03-12 13:00:00', 3, N'已完成', 100, 100, 100, 'CreditCard', 'AB-12345678', '/AB12345', 0, NULL),
    (2, 1, '003', 2, 2, '2024-11-15 12:30:00', '2024-11-15 15:30:00', 3, N'已完成', 50, 45, 45, 'LinePay', 'AB-12345679', '/CD67890', 0, NULL),
    (3, NULL, '005', 3, 3, '2024-05-20 09:00:00', '2024-05-20 13:00:00', 4, N'已完成', 300, 300, 300, 'Cash', 'AB-12345680', NULL, 0, NULL),
    (4, NULL, '007', 4, 4, '2024-08-10 14:00:00', '2024-08-10 18:00:00', 4, N'已完成', 50, 50, 50, 'ApplePay', 'AB-12345681', '/EF11223', 0, NULL),
    (5, 2, '001', 1, 1, '2023-01-05 18:00:00', '2023-01-05 23:00:00', 5, N'已完成', 100, 80, 80, 'CreditCard', 'AB-12345682', NULL, 0, NULL),

    -- 6~10: 甲地租乙地還 (已完成)
    (6, NULL, '009', 5, 6, '2023-04-22 10:00:00', '2023-04-22 11:30:00', 2, N'已完成', 150, 150, 150, 'LinePay', 'AB-12345683', '/GH33445', 0, N'甲租乙還'),
    (7, NULL, '011', 7, 8, '2023-12-15 15:00:00', '2023-12-15 17:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'AB-12345684', NULL, 0, NULL),
    (8, 3, '013', 9, 10, '2024-02-14 08:00:00', '2024-02-14 10:00:00', 2, N'已完成', 100, 90, 90, 'CreditCard', 'AB-12345685', '/IJ55667', 0, NULL),
    (9, NULL, '002', 1, 3, '2024-06-30 20:00:00', '2024-07-01 00:00:00', 4, N'已完成', 200, 200, 200, 'JKOPay', 'AB-12345686', NULL, 0, NULL),
    (10, NULL, '004', 2, 4, '2024-09-12 13:00:00', '2024-09-12 15:00:00', 2, N'已完成', 80, 80, 80, 'ApplePay', 'AB-12345687', '/KL77889', 0, NULL),

    -- 11~15: 租借中 (Active) - 保持目前時間
    (10, NULL, '002', 1, NULL, SYSDATETIME(), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (3, NULL, '047', 14, NULL, DATEADD(MINUTE, -30, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (7, NULL, '111', 7, NULL, DATEADD(HOUR, -1, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (4, NULL, '410', 56, NULL, DATEADD(HOUR, -2, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, N'長時間使用'),
    (11, NULL, '068', 49, NULL, DATEADD(MINUTE, -10, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (9, NULL, '007', 4, NULL, DATEADD(MINUTE, -30, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (2, NULL, '051', 7, NULL, DATEADD(HOUR, -1, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (4, NULL, '115', 56, NULL, DATEADD(HOUR, -2, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, N'長時間使用'),
    (9, NULL, '018', 34, NULL, DATEADD(MINUTE, -10, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (6, NULL, '157', 4, NULL, DATEADD(MINUTE, -30, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (5, NULL, '011', 57, NULL, DATEADD(HOUR, -1, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (4, NULL, '011', 56, NULL, DATEADD(HOUR, -2, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, N'長時間使用'),
    (1, NULL, '168', 48, NULL, DATEADD(MINUTE, -10, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (3, NULL, '127', 45, NULL, DATEADD(MINUTE, -30, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (8, NULL, '411', 17, NULL, DATEADD(HOUR, -1, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),
    (10, NULL, '310', 56, NULL, DATEADD(HOUR, -2, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, N'長時間使用'),
    (11, NULL, '058', 64, NULL, DATEADD(MINUTE, -10, SYSDATETIME()), NULL, NULL, N'租借中', NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL),

    -- 16~18: 異常或違規紀錄
    (1, NULL, '006', 3, 3, '2021-07-01 10:00:00', '2021-07-01 14:00:00', 4, N'已完成', 400, 400, 400, 'Cash', 'AB-12345688', NULL, 1, N'超時歸還'),
    (7, NULL, '004', 2, NULL, '2024-10-05 09:00:00', NULL, NULL, N'未歸還', 0, 0, 0, NULL, NULL, NULL, 2, N'惡意遺失'),
    (8, NULL, '012', 8, 8, '2023-03-20 11:00:00', '2023-03-20 11:10:00', 1, N'已取消', 0, 0, 0, NULL, NULL, NULL, 0, N'設備故障取消'),

    -- 19~20: 跨日租借
    (9, NULL, '014', 10, 10, '2024-11-10 23:00:00', '2024-11-11 02:00:00', 3, N'已完成', 200, 200, 200, 'CreditCard', 'AB-12345689', NULL, 0, N'跨日租借'),
    (2, 5, '001', 1, 2, '2025-06-12 23:50:00', '2025-06-12 01:50:00', 2, N'已完成', 50, 40, 40, 'LinePay', 'AB-12345690', NULL, 0, NULL),

    -- 21~30: 額外隨機數據 (2021-2025)
    (1, NULL, '001', 1, 1, '2021-09-06 10:00:00', '2021-09-06 11:00:00', 1, N'已完成', 100, 100, 100, 'CreditCard', 'AB-12345678', '/AB12345', 0, NULL),
    (1, 1, '003', 2, 2, '2024-11-01 12:30:00', '2024-11-01 13:30:00', 1, N'已完成', 50, 45, 45, 'LinePay', 'AB-12345679', '/CD67890', 0, NULL),
    (1, NULL, '005', 3, 3, '2023-06-06 09:00:00', '2023-06-06 11:00:00', 2, N'已完成', 300, 300, 300, 'Cash', 'AB-12345680', NULL, 0, NULL),
    (1, NULL, '007', 4, 4, '2024-11-11 14:00:00', '2024-11-11 15:00:00', 1, N'已完成', 50, 50, 50, 'ApplePay', 'AB-12345681', '/EF11223', 0, NULL),
    (5, 2, '001', 1, 1, '2025-06-08 18:00:00', '2025-06-08 20:00:00', 2, N'已完成', 100, 80, 80, 'CreditCard', 'AB-12345682', NULL, 0, NULL),
    (3, NULL, '009', 5, 6, '2024-05-05 10:00:00', '2024-05-05 11:30:00', 1, N'已完成', 150, 150, 150, 'LinePay', 'AB-12345683', '/GH33445', 0, N'甲租乙還'),
    (3, NULL, '011', 7, 8, '2024-10-13 15:00:00', '2024-10-13 18:00:00', 3, N'已完成', 100, 100, 100, 'Cash', 'AB-12345684', NULL, 0, NULL),
    (8, 3, '013', 9, 10, '2023-11-07 08:00:00', '2023-11-07 12:00:00', 4, N'已完成', 100, 90, 90, 'CreditCard', 'AB-12345685', '/IJ55667', 0, NULL),
    (9, NULL, '002', 1, 3, '2024-10-28 20:00:00', '2024-10-29 00:00:00', 4, N'已完成', 200, 200, 200, 'JKOPay', 'AB-12345686', NULL, 0, NULL),
    (10, NULL, '004', 2, 4, '2025-06-12 13:00:00', '2025-06-12 15:00:00', 2, N'已完成', 80, 80, 80, 'ApplePay', 'AB-12345687', '/KL77889', 0, NULL),
    (2, NULL, '002', 1, 1, '2021-02-14 09:00:00', '2021-02-14 11:30:00', 3, N'已完成', 150, 150, 150, 'CreditCard', 'BK-20210001', NULL, 0, NULL),
    (5, 1, '010', 5, 5, '2021-03-20 14:00:00', '2021-03-20 15:00:00', 1, N'已完成', 50, 40, 40, 'LinePay', 'BK-20210002', '/TW123', 0, NULL),
    (8, NULL, '015', 3, 4, '2021-11-12 10:00:00', '2021-11-12 14:00:00', 4, N'已完成', 200, 200, 200, 'ApplePay', 'BK-20210003', NULL, 0, N'甲租乙還'),
    (1, NULL, '004', 2, 2, '2021-06-01 08:00:00', '2021-06-01 12:00:00', 4, N'已完成', 200, 200, 200, 'Cash', 'BK-20210004', NULL, 0, NULL),
    (4, 3, '008', 4, 4, '2021-07-15 18:30:00', '2021-07-15 20:30:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20210005', '/QR556', 0, NULL),
    (10, NULL, '012', 6, 6, '2021-08-22 13:00:00', '2021-08-22 17:00:00', 4, N'已完成', 200, 200, 200, 'JKOPay', 'BK-20210006', NULL, 1, N'超時歸還'),
    (3, NULL, '003', 9, 9, '2021-09-10 11:00:00', '2021-09-10 12:00:00', 1, N'已完成', 50, 50, 50, 'CreditCard', 'BK-20210007', '/AA111', 0, NULL),
    (6, 2, '001', 1, 2, '2021-10-05 09:00:00', '2021-10-05 11:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20210008', NULL, 0, N'甲租乙還'),
    (9, NULL, '007', 7, 7, '2021-11-11 15:00:00', '2021-11-11 18:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20210009', NULL, 0, NULL),
    (2, NULL, '014', 10, 10, '2021-12-24 20:00:00', '2021-12-25 01:00:00', 5, N'已完成', 250, 250, 250, 'LinePay', 'BK-20210010', '/CC990', 0, N'跨日租借'),
    (7, NULL, '005', 3, 3, '2021-12-31 22:00:00', '2024-01-01 02:00:00', 4, N'已完成', 200, 200, 200, 'CreditCard', 'BK-20210011', NULL, 0, N'跨日租借'),
    (5, NULL, '011', 8, 8, '2021-04-18 10:00:00', '2021-04-18 10:15:00', 1, N'已取消', 0, 0, 0, NULL, NULL, NULL, 0, N'臨時取消'),

    -- 2022 年資料 (12筆)
    (1, NULL, '009', 5, 5, '2024-01-20 10:00:00', '2024-01-20 12:00:00', 2, N'已完成', 100, 100, 100, 'LinePay', 'BK-20220001', '/BB222', 0, NULL),
    (3, 4, '013', 9, 10, '2024-02-14 17:00:00', '2024-02-14 19:00:00', 2, N'已完成', 100, 80, 80, 'ApplePay', 'BK-20220002', NULL, 0, N'甲租乙還'),
    (10, NULL, '006', 4, 4, '2024-03-30 08:30:00', '2024-03-30 11:30:00', 3, N'已完成', 150, 150, 150, 'CreditCard', 'BK-20220003', NULL, 0, NULL),
    (2, NULL, '002', 1, 1, '2024-04-12 14:00:00', '2024-04-12 16:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20220004', NULL, 0, NULL),
    (6, 1, '011', 7, 7, '2024-05-05 10:00:00', '2024-05-05 13:00:00', 3, N'已完成', 150, 135, 135, 'JKOPay', 'BK-20220005', '/JJ888', 0, NULL),
    (8, NULL, '001', 1, 3, '2024-11-11 15:00:00', '2024-11-11 18:00:00', 3, N'已完成', 150, 150, 150, 'CreditCard', 'BK-20220006', NULL, 0, N'甲租乙還'),
    (4, NULL, '004', 2, 2, '2024-07-07 09:00:00', '2024-07-07 10:30:00', 2, N'已完成', 100, 100, 100, 'ApplePay', 'BK-20220007', '/AA001', 0, NULL),
    (9, NULL, '015', 3, 3, '2024-11-11 12:00:00', '2024-11-11 15:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20220008', NULL, 0, NULL),
    (5, 2, '008', 4, 1, '2024-09-22 18:00:00', '2024-09-22 21:00:00', 3, N'已完成', 150, 120, 120, 'LinePay', 'BK-20220009', NULL, 0, N'甲租乙還'),
    (7, NULL, '007', 6, 6, '2024-10-10 10:00:00', '2024-10-10 14:00:00', 4, N'已完成', 200, 200, 200, 'CreditCard', 'BK-20220010', '/MM123', 1, N'逾時'),
    (1, NULL, '012', 8, 8, '2024-11-11 11:11:00', '2024-11-11 13:11:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20220011', NULL, 0, NULL),
    (2, NULL, '010', 5, NULL, '2024-12-20 14:00:00', NULL, NULL, N'未歸還', 0, 0, 0, NULL, NULL, NULL, 2, N'遺失'),

    -- 2023 年資料 (12筆)
    (3, NULL, '003', 2, 2, '2023-01-15 09:00:00', '2023-01-15 11:00:00', 2, N'已完成', 100, 100, 100, 'ApplePay', 'BK-20230001', NULL, 0, NULL),
    (5, 5, '001', 1, 1, '2023-02-28 13:00:00', '2023-02-28 15:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20230002', '/GG777', 0, NULL),
    (9, NULL, '014', 10, 4, '2023-03-12 10:00:00', '2023-03-12 14:00:00', 4, N'已完成', 200, 200, 200, 'CreditCard', 'BK-20230003', NULL, 0, N'甲租乙還'),
    (1, NULL, '005', 3, 3, '2023-04-18 08:30:00', '2023-04-18 10:30:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20230004', NULL, 0, NULL),
    (4, NULL, '012', 8, 8, '2023-05-20 15:00:00', '2023-05-20 17:00:00', 2, N'已完成', 100, 100, 100, 'LinePay', 'BK-20230005', '/PP001', 0, NULL),
    (6, NULL, '008', 4, 4, '2023-06-15 09:00:00', '2023-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20230006', NULL, 0, NULL),
    (8, 2, '011', 7, 7, '2023-07-04 14:00:00', '2023-07-04 16:00:00', 2, N'已完成', 100, 80, 80, 'JKOPay', 'BK-20230007', '/KK222', 0, NULL),
    (10, NULL, '002', 1, 5, '2023-08-10 10:00:00', '2023-08-10 13:00:00', 3, N'已完成', 150, 150, 150, 'CreditCard', 'BK-20230008', NULL, 0, N'甲租乙還'),
    (2, NULL, '004', 2, 2, '2023-11-11 11:00:00', '2023-11-11 13:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20230009', NULL, 0, NULL),
    (7, 3, '015', 3, 3, '2023-10-31 18:00:00', '2023-10-31 21:00:00', 3, N'已完成', 150, 130, 130, 'LinePay', 'BK-20230010', '/YY555', 0, NULL),
    (1, NULL, '007', 6, 6, '2023-11-20 09:00:00', '2023-11-20 12:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20230011', NULL, 0, NULL),
    (4, NULL, '013', 9, 9, '2023-12-25 14:00:00', '2023-12-25 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20230012', NULL, 0, NULL),

    -- 2024 年資料 (12筆)
    (5, NULL, '002', 1, 1, '2024-01-10 10:00:00', '2024-01-10 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20240001', NULL, 0, NULL),
    (8, 1, '010', 5, 2, '2024-02-11 13:00:00', '2024-02-11 16:00:00', 3, N'已完成', 150, 130, 130, 'LinePay', 'BK-20240002', '/UU111', 0, N'甲租乙還'),
    (10, NULL, '005', 3, 3, '2024-03-20 09:00:00', '2024-03-20 11:00:00', 2, N'已完成', 100, 100, 100, 'ApplePay', 'BK-20240003', NULL, 0, NULL),
    (3, NULL, '012', 8, 8, '2024-11-11 15:00:00', '2024-11-11 18:00:00', 3, N'已完成', 150, 150, 150, 'CreditCard', 'BK-20240004', NULL, 0, NULL),
    (6, 4, '009', 5, 5, '2024-05-12 10:00:00', '2024-05-12 12:00:00', 2, N'已完成', 100, 80, 80, 'JKOPay', 'BK-20240005', '/RR444', 0, NULL),
    (1, NULL, '001', 1, 1, '2024-11-11 08:30:00', '2024-11-11 10:30:00', 2, N'已完成', 100, 100, 100, 'LinePay', 'BK-20240006', NULL, 0, NULL),

    (5, NULL, '002', 1, 1, '2024-01-10 10:00:00', '2024-01-10 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20240001', NULL, 0, NULL),
    (8, 1, '010', 5, 2, '2024-02-11 13:00:00', '2024-02-11 16:00:00', 3, N'已完成', 150, 130, 130, 'LinePay', 'BK-20240002', '/UU111', 0, N'甲租乙還'),
    (10, NULL, '005', 3, 3, '2024-03-20 09:00:00', '2024-03-20 11:00:00', 2, N'已完成', 100, 100, 100, 'ApplePay', 'BK-20240003', NULL, 0, NULL),
    (3, NULL, '012', 8, 8, '2024-11-11 15:00:00', '2024-11-11 18:00:00', 3, N'已完成', 150, 150, 150, 'CreditCard', 'BK-20240004', NULL, 0, NULL),
    (6, 4, '009', 5, 5, '2024-05-12 10:00:00', '2024-05-12 12:00:00', 2, N'已完成', 100, 80, 80, 'JKOPay', 'BK-20240005', '/RR444', 0, NULL),
    (1, NULL, '001', 1, 1, '2024-11-11 08:30:00', '2024-11-11 10:30:00', 2, N'已完成', 100, 100, 100, 'LinePay', 'BK-20240006', NULL, 0, NULL),
    (9, NULL, '014', 10, 10, '2024-11-11 19:00:00', '2024-11-11 22:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20240007', NULL, 0, NULL),
    (2, 2, '011', 7, 7, '2024-11-11 14:00:00', '2024-11-11 16:00:00', 2, N'已完成', 100, 90, 90, 'CreditCard', 'BK-20240008', '/EE555', 0, NULL),
    (4, NULL, '003', 2, 6, '2024-09-15 10:00:00', '2024-09-15 13:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20240009', NULL, 0, N'甲租乙還'),
    (7, NULL, '008', 4, 4, '2024-11-11 09:00:00', '2024-11-11 11:00:00', 2, N'已完成', 100, 100, 100, 'LinePay', 'BK-20240010', NULL, 0, NULL),
    (10, NULL, '006', 4, 4, '2024-11-11 15:00:00', '2024-11-11 17:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20240011', NULL, 0, NULL),
    (9, NULL, '014', 10, 10, '2024-11-11 19:00:00', '2024-11-11 22:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20240007', NULL, 0, NULL),
    (2, 2, '011', 7, 7, '2024-11-11 14:00:00', '2024-11-11 16:00:00', 2, N'已完成', 100, 90, 90, 'CreditCard', 'BK-20240008', '/EE555', 0, NULL),
    (4, NULL, '003', 2, 6, '2024-09-15 10:00:00', '2024-09-15 13:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20240009', NULL, 0, N'甲租乙還'),
    (7, NULL, '008', 4, 4, '2024-11-11 09:00:00', '2024-11-11 11:00:00', 2, N'已完成', 100, 100, 100, 'LinePay', 'BK-20240010', NULL, 0, NULL),
    (10, NULL, '006', 4, 4, '2024-11-11 15:00:00', '2024-11-11 17:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20240011', NULL, 0, NULL),
    (5, NULL, '015', 3, 3, '2024-12-24 21:00:00', '2024-12-25 00:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20240012', NULL, 0, N'聖誕跨夜'),

    -- 2025 年資料 (12筆)
    (1, 1, '001', 1, 13, '2025-06-05 10:00:00', '2025-06-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-05-10 14:00:00', '2025-05-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-05-15 09:00:00', '2025-05-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-05-18 15:00:00', '2025-05-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 42, 52, '2025-05-20 10:00:00', '2025-05-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-05-21 19:00:00', '2025-06-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 14, 14, '2025-06-25 08:30:00', '2025-06-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 11, 11, '2025-06-26 13:00:00', '2025-06-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 33, 33, '2025-06-27 10:00:00', '2025-06-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, 1, '001', 21, 21, '2025-06-05 10:00:00', '2025-06-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 45, 55, '2025-06-10 14:00:00', '2025-06-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (1, 1, '001', 21, 21, '2025-06-05 10:00:00', '2025-06-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 45, 55, '2025-06-10 14:00:00', '2025-06-10 16:40:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-10 14:00:00', '2025-06-10 16:40:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-10 14:00:00', '2025-06-10 16:40:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-10 14:00:00', '2025-06-10 16:40:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-10 14:00:00', '2025-06-10 16:40:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-10 14:00:00', '2025-06-10 16:40:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-10 14:00:00', '2025-06-10 16:40:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-10 14:00:00', '2025-06-10 16:40:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-10 14:00:00', '2025-06-10 16:40:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 88, '2025-10-18 15:00:00', '2025-10-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 24, 44, '2025-10-25 08:30:00', '2025-12-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 41, 31, '2025-12-26 13:00:00', '2025-12-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-27 10:00:00', '2025-12-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (1, 1, '001', 1, 13, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-12-10 14:00:00', '2025-12-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-12-18 15:00:00', '2025-12-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 42, 52, '2025-12-20 10:00:00', '2025-12-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 88, '2025-10-18 15:00:00', '2025-10-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 24, 44, '2025-10-25 08:30:00', '2025-12-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 41, 31, '2025-12-26 13:00:00', '2025-12-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-27 10:00:00', '2025-12-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (1, 1, '001', 1, 13, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-12-10 14:00:00', '2025-12-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-12-18 15:00:00', '2025-12-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 88, '2025-10-18 15:00:00', '2025-10-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 24, 44, '2025-10-25 08:30:00', '2025-12-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 41, 31, '2025-12-26 13:00:00', '2025-12-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-23 10:00:00', '2025-12-23 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-12-22 14:00:00', '2025-12-22 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-24 09:00:00', '2025-12-24 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-12-24 14:00:00', '2025-12-24 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (1, 1, '001', 1, 13, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-12-10 14:00:00', '2025-12-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-12-18 15:00:00', '2025-12-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (2, NULL, '014', 10, 10, '2025-12-21 19:00:00', '2025-12-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 14, 14, '2025-12-25 08:30:00', '2025-12-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-12-21 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-28 09:00:00', '2025-12-28 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-12-25 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-12-28 09:00:00', '2025-12-28 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (1, 1, '001', 1, 13, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-12-10 14:00:00', '2025-12-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 88, '2025-10-18 15:00:00', '2025-10-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 24, 44, '2025-10-25 08:30:00', '2025-12-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 41, 31, '2025-12-26 13:00:00', '2025-12-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-24 10:00:00', '2025-12-24 12:50:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-12-17 14:00:00', '2025-12-17 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-26 09:00:00', '2025-12-26 11:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-26 09:00:00', '2025-12-26 11:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-26 09:00:00', '2025-12-26 11:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-26 09:00:00', '2025-12-26 11:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-26 09:00:00', '2025-12-26 11:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-26 09:00:00', '2025-12-26 11:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-12-17 09:00:00', '2025-12-17 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (1, 1, '001', 1, 13, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-12-10 14:00:00', '2025-12-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-12-18 15:00:00', '2025-12-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 42, 52, '2025-12-20 10:00:00', '2025-12-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 88, '2025-10-18 15:00:00', '2025-10-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 24, 44, '2025-10-25 08:30:00', '2025-12-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 41, 31, '2025-12-26 13:00:00', '2025-12-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-27 10:00:00', '2025-12-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (1, 1, '001', 1, 13, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-12-10 14:00:00', '2025-12-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-12-18 15:00:00', '2025-12-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 88, '2025-10-18 15:00:00', '2025-10-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-10-20 10:00:00', '2025-10-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-10-21 19:00:00', '2025-10-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 24, 44, '2025-10-25 08:30:00', '2025-12-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 41, 31, '2025-12-26 13:00:00', '2025-12-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 13, 3, '2025-12-27 10:00:00', '2025-12-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (1, 1, '001', 1, 13, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-12-10 14:00:00', '2025-12-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-12-18 15:00:00', '2025-12-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (2, NULL, '014', 10, 10, '2025-12-21 19:00:00', '2025-12-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 14, 14, '2025-12-25 08:30:00', '2025-12-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-12-27 14:00:00', '2025-12-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-12-27 09:00:00', '2025-12-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (1, 1, '001', 1, 13, '2025-12-05 10:00:00', '2025-12-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-12-10 14:00:00', '2025-12-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-12-15 09:00:00', '2025-12-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-12-18 15:00:00', '2025-12-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 42, 52, '2025-12-20 10:00:00', '2025-12-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-12-21 19:00:00', '2025-12-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),

    (1, 1, '001', 1, 13, '2025-06-05 10:00:00', '2025-06-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-06-10 14:00:00', '2025-06-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-06-18 15:00:00', '2025-06-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 42, 52, '2025-06-20 10:00:00', '2025-06-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-06-21 19:00:00', '2025-06-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 14, 14, '2025-06-25 08:30:00', '2025-06-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 11, 11, '2025-06-26 13:00:00', '2025-06-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 33, 33, '2025-06-27 10:00:00', '2025-06-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, 1, '001', 21, 21, '2025-06-05 10:00:00', '2025-06-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 45, 55, '2025-06-30 14:00:00', '2025-06-30 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-29 09:00:00', '2025-06-29 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-30 14:00:00', '2025-06-30 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-29 09:00:00', '2025-06-29 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-30 14:00:00', '2025-06-30 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-29 09:00:00', '2025-06-29 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-30 14:00:00', '2025-06-30 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-29 09:00:00', '2025-06-29 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-30 14:00:00', '2025-06-30 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-29 09:00:00', '2025-06-29 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-30 14:00:00', '2025-06-30 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-29 09:00:00', '2025-06-29 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-30 14:00:00', '2025-06-30 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-29 09:00:00', '2025-06-29 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-30 14:00:00', '2025-06-30 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-29 09:00:00', '2025-06-29 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (3, NULL, '010', 45, 55, '2025-06-30 14:00:00', '2025-06-30 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-06-29 09:00:00', '2025-06-29 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 88, '2025-06-18 15:00:00', '2025-06-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-06-20 10:00:00', '2025-06-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-06-21 19:00:00', '2025-06-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 24, 44, '2025-01-25 08:30:00', '2025-01-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 41, 31, '2025-01-26 13:00:00', '2025-01-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 13, 3, '2025-01-27 10:00:00', '2025-01-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-01-27 14:00:00', '2025-01-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-01-27 09:00:00', '2025-01-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-01-27 14:00:00', '2025-01-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-01-27 09:00:00', '2025-01-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-01-05 10:00:00', '2025-01-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (1, 1, '001', 1, 13, '2025-03-05 10:00:00', '2025-03-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-03-10 14:00:00', '2025-03-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-03-15 09:00:00', '2025-03-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-03-18 15:00:00', '2025-03-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 42, 52, '2025-03-20 10:00:00', '2025-03-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-09-21 19:00:00', '2025-09-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 14, 14, '2025-09-25 08:30:00', '2025-09-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 11, 11, '2025-09-26 13:00:00', '2025-09-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 33, 33, '2025-02-27 10:00:00', '2025-02-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, 1, '001', 21, 21, '2025-02-05 10:00:00', '2025-02-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 45, 55, '2025-02-10 14:00:00', '2025-02-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-02-15 09:00:00', '2025-02-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 88, '2025-06-18 15:00:00', '2025-06-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 22, 42, '2025-06-20 10:00:00', '2025-06-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (7, 5, '002', 41, 31, '2025-01-26 13:00:00', '2025-01-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 13, 3, '2025-01-27 10:00:00', '2025-01-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-01-27 14:00:00', '2025-01-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-01-27 09:00:00', '2025-01-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-01-27 14:00:00', '2025-01-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-01-27 09:00:00', '2025-01-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-01-05 10:00:00', '2025-01-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (1, 1, '001', 1, 13, '2025-03-05 10:00:00', '2025-03-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 15, '2025-03-10 14:00:00', '2025-03-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 14, '2025-03-15 09:00:00', '2025-03-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 48, '2025-03-18 15:00:00', '2025-03-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 42, 52, '2025-03-20 10:00:00', '2025-03-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-09-21 19:00:00', '2025-09-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 14, 14, '2025-09-25 08:30:00', '2025-09-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 11, 11, '2025-09-26 13:00:00', '2025-09-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 33, 33, '2025-02-27 10:00:00', '2025-02-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, 1, '001', 21, 21, '2025-02-05 10:00:00', '2025-02-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 45, 55, '2025-02-10 14:00:00', '2025-02-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 13, 54, '2025-02-15 09:00:00', '2025-02-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 88, '2025-06-18 15:00:00', '2025-06-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (2, NULL, '014', 10, 10, '2025-06-21 19:00:00', '2025-06-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 24, 44, '2025-06-25 08:30:00', '2025-06-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 41, 31, '2025-06-26 13:00:00', '2025-06-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 13, 3, '2025-06-27 10:00:00', '2025-06-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, NULL, '007', 46, 56, '2025-06-27 14:00:00', '2025-06-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 24, '2025-06-27 09:00:00', '2025-06-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 16, 46, '2025-06-27 14:00:00', '2025-06-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 14, 14, '2025-06-27 09:00:00', '2025-06-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, 1, '001', 12, 21, '2025-06-05 10:00:00', '2025-06-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 5, '2025-06-10 14:00:00', '2025-06-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 4, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 8, '2025-06-18 15:00:00', '2025-06-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 2, 2, '2025-06-20 10:00:00', '2025-06-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-06-21 19:00:00', '2025-06-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 4, 4, '2025-06-25 08:30:00', '2025-06-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 1, 1, '2025-06-26 13:00:00', '2025-06-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 3, 3, '2025-06-27 10:00:00', '2025-06-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, 1, '001', 1, 1, '2025-06-05 10:00:00', '2025-06-05 12:00:00', 2, N'已完成', 100, 80, 80, 'LinePay', 'BK-20250001', '/ZZ111', 0, NULL),
    (3, NULL, '010', 5, 5, '2025-06-10 14:00:00', '2025-06-10 16:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250002', NULL, 0, NULL),
    (6, NULL, '005', 3, 4, '2025-06-15 09:00:00', '2025-06-15 12:00:00', 3, N'已完成', 150, 150, 150, 'Cash', 'BK-20250003', NULL, 0, N'甲租乙還'),
    (8, NULL, '012', 8, 8, '2025-06-18 15:00:00', '2025-06-18 18:00:00', 3, N'已完成', 150, 150, 150, 'ApplePay', 'BK-20250004', NULL, 0, NULL),
    (10, 2, '003', 2, 2, '2025-06-20 10:00:00', '2025-06-20 12:00:00', 2, N'已完成', 100, 90, 90, 'LinePay', 'BK-20250005', '/TT666', 0, NULL),
    (2, NULL, '014', 10, 10, '2025-06-21 19:00:00', '2025-06-22 21:00:00', 2, N'已完成', 100, 100, 100, 'JKOPay', 'BK-20250006', NULL, 0, NULL),
    (4, NULL, '008', 4, 4, '2025-06-25 08:30:00', '2025-06-25 10:30:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250007', NULL, 0, NULL),
    (7, 5, '002', 1, 1, '2025-06-26 13:00:00', '2025-06-26 15:00:00', 2, N'已完成', 100, 90, 90, 'ApplePay', 'BK-20250008', '/WW888', 0, NULL),
    (9, NULL, '015', 3, 3, '2025-06-27 10:00:00', '2025-06-27 12:00:00', 2, N'已完成', 100, 100, 100, 'Cash', 'BK-20250009', NULL, 0, NULL),
    (1, NULL, '007', 6, 6, '2025-06-27 14:00:00', '2025-06-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 4, 4, '2025-06-27 09:00:00', '2025-06-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (1, NULL, '007', 6, 6, '2025-06-27 14:00:00', '2025-06-27 17:00:00', 3, N'已完成', 150, 150, 150, 'LinePay', 'BK-20250010', NULL, 0, NULL),
    (5, NULL, '006', 4, 4, '2025-06-27 09:00:00', '2025-06-27 11:00:00', 2, N'已完成', 100, 100, 100, 'CreditCard', 'BK-20250011', NULL, 0, NULL),
    (2, NULL, '011', 7, 7, '2025-06-30 15:00:00', '2025-06-30 18:00:00', 3, N'已完成', 150, 150, 150, 'JKOPay', 'BK-20250012', NULL, 0, NULL);
--SQL VIEWS
USE [SeatRentSys];
GO
CREATE VIEW V_RentDetails
AS
    SELECT
        r.recId, r.memId, m.memName, r.couponId, d.couponDescription couponDesc, r.seatsId, r.spotIdRent,
        s1.spotName AS RentSpotName, r.spotIdReturn, s2.spotName AS ReturnSpotName,
        r.recRentDT2, r.recReturnDT2, r.recUsageDT2, r.recPrice, r.recRequestPay, r.recPayment, r.recPayBy,
        r.recInvoice, r.recCarrier, r.recViolatInt, r.recNote, r.recStatus
    FROM recRent r
        -- 1. 關聯會員 (必定存在，使用 INNER JOIN)
        INNER JOIN member m ON r.memId = m.memId
        LEFT JOIN discount d ON r.couponId = d.couponId
        -- 2. 關聯租借點 (必定存在，使用 INNER JOIN)
        INNER JOIN renting_Spot s1 ON r.spotIdRent = s1.spotId
        -- 3. 關聯歸還點 (可能未還，使用 LEFT JOIN)
        LEFT JOIN renting_Spot s2 ON r.spotIdReturn = s2.spotId;
--==================子桓 DATA END====================
--=================DB BUILD END====================
