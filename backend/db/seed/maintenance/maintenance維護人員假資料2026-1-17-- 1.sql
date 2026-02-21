--===================維修人員假資料========================



-- 先清空舊資料 (如果您是剛建好表，這行可以忽略)
TRUNCATE TABLE [dbo].[maintenanceStaff];
GO

USE [SeatRentSys];
GO

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
(N'郭美玲', NULL, '0987-654-321', 'meiling.kuo@hotmail.com', N'合作已終止，暫不派案', 0),
(N'曾國華', N'順風家電維修', '0935-112-233', 'kh.tseng@wind-fix.com', N'一般電器設備更換與維修', 1),
(N'廖俊傑', N'全能修繕工程', '0918-999-000', 'jj.liao@all-fix.com', N'桌椅結構損壞修補與更換', 1),
(N'賴秀英', N'美好環境維護', '0920-555-123', 'hsiuying.lai@nice-env.com', N'負責垃圾清運與資源回收分類', 1),
(N'徐文雄', N'金鑰匙鎖印行', '0970-111-999', 'wh.hsu@key-lock.tw', N'電子鎖電池更換與開鎖服務', 1),
(N'蘇郁婷', N'連線通科技', '0916-222-888', 'yuting.su@connect-tech.com', N'路由器與交換器硬體設定', 1);
GO



--===================奕穎加的暫放=======
ALTER TABLE admin
ADD admStatus TINYINT NOT NULL DEFAULT 1;