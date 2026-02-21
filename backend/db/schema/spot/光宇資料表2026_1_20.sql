/* =========================================================
   1) 建立renting_Spot
   ========================================================= */
CREATE TABLE renting_Spot
(
    spotId       INT IDENTITY(1,1) NOT NULL,
    spotCode     VARCHAR(30)        NOT NULL,
    spotName     NVARCHAR(100)      NOT NULL,
    spotAddress  NVARCHAR(200)      NULL,
    spotStatus   NVARCHAR(20)       NOT NULL,
    merchantId   INT               NULL,

    createdAt    DATETIME2(7)       NOT NULL
        CONSTRAINT DF_renting_Spot_createdAt DEFAULT (SYSDATETIME()),

    updatedAt    DATETIME2(7)       NOT NULL
        CONSTRAINT DF_renting_Spot_updatedAt DEFAULT (SYSDATETIME()),

    latitude     DECIMAL(10,7)      NULL,
    longitude    DECIMAL(10,7)      NULL,
    spotDescrip  NVARCHAR(500)      NULL,
    spotImage    VARCHAR(255)       NULL,

    CONSTRAINT PK_renting_Spot PRIMARY KEY CLUSTERED (spotId)
);
GO

/* spotCode 唯一索引（對應檔案中的 UQ__renting__...） */
CREATE UNIQUE NONCLUSTERED INDEX UQ_renting_Spot_spotCode
ON dbo.renting_Spot (spotCode);
GO


/* =========================================================
   2) 建立seats
   ========================================================= */
CREATE TABLE seats
(
    seatsId       INT IDENTITY(1,1)  NOT NULL,
    seatsName     NVARCHAR(100)      NOT NULL,
    seatsType     NVARCHAR(50)       NOT NULL,
    seatsStatus   NVARCHAR(20)       NOT NULL,

    spotId        INT               NULL,

    updatedAt     DATETIME2(7)       NOT NULL
        CONSTRAINT DF_seats_updatedAt DEFAULT (SYSDATETIME()),

    serialNumber  VARCHAR(50)        NULL,

    createdAt     DATETIME2(7)       NOT NULL
        CONSTRAINT DF_seats_createdAt DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_seats PRIMARY KEY CLUSTERED (seatsId),

    CONSTRAINT FK_seats_spot
        FOREIGN KEY (spotId)
        REFERENCES dbo.renting_Spot (spotId)
);
GO

/* 一般索引：加速以 spotId 查 seats（對應 IX_seats_spotId） */
CREATE NONCLUSTERED INDEX IX_seats_spotId
ON dbo.seats (spotId);
GO

/* =========================================================
   serialNumber 唯一索引（對應 UQ_seats_spot_serialNumber）
   注意：serialNumber 允許 NULL；用「Filtered Unique Index」
   可避免 NULL 值造成唯一限制干擾。
   ========================================================= */
CREATE UNIQUE NONCLUSTERED INDEX UQ_seats_spot_serialNumber
ON dbo.seats (serialNumber)
WHERE serialNumber IS NOT NULL;
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
