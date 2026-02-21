USE [SeatRentSys];
GO

CREATE TABLE [dbo].[maintenanceInformation] (
    [ticketId]        INT            IDENTITY (1, 1) NOT NULL,
    [spotId]          INT            NULL,
    [issueType]       NVARCHAR (200) NOT NULL,
    [issueDesc]       NVARCHAR (500) NULL,
    [issuePriority]   VARCHAR (100)  DEFAULT ('NORMAL') NOT NULL,
    [issueStatus]     VARCHAR (50)   DEFAULT ('REPORTED') NOT NULL,
    [assignedStaffId] INT            NULL,
    [reportedAt]      DATETIME2 (7)  DEFAULT (sysdatetime()) NOT NULL,
    [startAt]         DATETIME2 (7)  NULL,
    [resolvedAt]      DATETIME2 (7)  NULL,
    [resolveNote]     NVARCHAR (500) NULL,
    [resultType]      NVARCHAR (50)  NULL,
    [seatsId]         INT            NULL,
    PRIMARY KEY CLUSTERED ([ticketId] ASC),
    CONSTRAINT [FK_maintenanceInformation_seats] FOREIGN KEY ([seatsId]) REFERENCES [dbo].[seats] ([seatsId]),
    CONSTRAINT [fkMaintAssignedStaffId] FOREIGN KEY ([assignedStaffId]) REFERENCES [dbo].[maintenanceStaff] ([staffId])
);



--=================================
USE [SeatRentSys];
GO

CREATE TABLE [dbo].[maintenanceStaff] (
    [staffId]      INT            IDENTITY (1, 1) NOT NULL,
    [staffName]    NVARCHAR (50)  NOT NULL,
    [staffCompany] NVARCHAR (100) NULL,
    [staffPhone]   VARCHAR (20)   NULL,
    [staffEmail]   VARCHAR (100)  NULL,
    [staffNote]    NVARCHAR (200) NULL,
    [createdAt]    DATETIME2 (7)  DEFAULT (sysdatetime()) NOT NULL,
    [isActive]     BIT            CONSTRAINT [DF_maintenanceStaff_isActive] DEFAULT ((1)) NOT NULL,
    PRIMARY KEY CLUSTERED ([staffId] ASC)
);
--===================================================
USE [SeatRentSys];
GO
-- 建立排程表
CREATE TABLE [dbo].[maintenanceSchedule] (
    [scheduleId]       INT            IDENTITY (1, 1) NOT NULL,
    [title]            NVARCHAR (100) NOT NULL,    -- 任務名稱
    
    -- 目標設定 (Polymorphic Association)
    [targetType]       VARCHAR (20)   NOT NULL,    -- 'SPOT' 或 'SEAT'
    [targetId]         INT            NOT NULL,    -- 對應 renting_Spot.spotId 或 seats.seatsId
    
    -- 頻率設定
    [scheduleType]     VARCHAR (20)   NOT NULL,    -- 'DAILY', 'WEEKLY', 'MONTHLY'
    [dayOfWeek]        INT            NULL,        -- 1-7 (週排程用)
    [dayOfMonth]       INT            NULL,        -- 1-31 (月排程用)
    [executeTime]      TIME (7)       NOT NULL,    -- 預計執行時間
    
    -- 工單內容預設值
    [issueType]        NVARCHAR (200) NOT NULL,
    [issuePriority]    VARCHAR (50)   DEFAULT ('NORMAL') NOT NULL,
    [assignedStaffId]  INT            NULL,        -- 預設指派的虛擬廠商ID
    
    -- 排程控制
    [isActive]         BIT            DEFAULT ((1)) NOT NULL,
    [lastExecutedAt]   DATETIME2 (7)  NULL,
    [nextExecuteAt]    DATETIME2 (7)  NOT NULL,    -- 系統自動計算的下次執行時間
    
    [createdAt]        DATETIME2 (7)  DEFAULT (sysdatetime()) NOT NULL,
    [updatedAt]        DATETIME2 (7)  DEFAULT (sysdatetime()) NOT NULL,

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
-- 整合版：包含 Idempotency 檢查與效能索引
-- =============================================
USE [SeatRentSys];
GO
-- 1. 建立資料表 (如果表不存在才建立)
IF OBJECT_ID(N'dbo.maintenanceLog', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[maintenanceLog] (
        [logId]     INT            IDENTITY (1, 1) NOT NULL, -- 流水號 PK
        [ticketId]  INT            NOT NULL,                 -- 關聯原本的工單 ID
        [operator]  NVARCHAR (50)  NOT NULL,                 -- 操作者 (支援中文)
        [action]    VARCHAR (50)   NOT NULL,                 -- 動作代號 (英文)
        [comment]   NVARCHAR (500) NULL,                     -- 詳細說明
        [createdAt] DATETIME2 (7)  DEFAULT (sysdatetime()) NOT NULL, -- 發生時間 (預設當下)

        -- 設定主鍵 (Primary Key)
        CONSTRAINT [PK_maintenanceLog] PRIMARY KEY CLUSTERED ([logId] ASC)
    );


-- 2. 建立外鍵關聯 (如果 FK 不存在才建立)
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_maintenanceLog_ticket')
BEGIN
    ALTER TABLE [dbo].[maintenanceLog]
    ADD CONSTRAINT [FK_maintenanceLog_ticket] 
    FOREIGN KEY ([ticketId]) 
    REFERENCES [dbo].[maintenanceInformation] ([ticketId])
    ON DELETE CASCADE; -- 工單刪除時，歷史紀錄一併刪除
    PRINT ' 外鍵 FK_maintenanceLog_ticket 建立成功';
END;

-- 3. 建立查詢索引 (針對 Timeline 優化)
-- 這會讓 "SELECT * FROM logs WHERE ticketId = ? ORDER BY createdAt DESC" 飛快
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_maintenanceLog_ticket_createdAt')
BEGIN
    CREATE NONCLUSTERED INDEX [IX_maintenanceLog_ticket_createdAt]
    ON [dbo].[maintenanceLog] ([ticketId] ASC, [createdAt] DESC);
    PRINT '效能索引 IX_maintenanceLog_ticket_createdAt 建立成功';
END;
GO