# Coze Web Chat SDK 快速設定腳本
# 用途：協助快速設定 Coze 環境變數並驗證配置

param(
    [Parameter(Mandatory=$false)]
    [string]$BotId,
    
    [Parameter(Mandatory=$false)]
    [string]$Pat,
    
    [Parameter(Mandatory=$false)]
    [string]$SdkSrc = "https://lf-cdn.coze.cn/obj/unpkg/flow-platform/chat-app-sdk/1.0.0-beta.4/libs/oversea/index.js"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Coze Web Chat SDK 快速設定工具" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# ==================== Step 1：收集參數 ====================
if (-not $BotId) {
    Write-Host "請輸入 Coze Bot ID：" -ForegroundColor Yellow -NoNewline
    $BotId = Read-Host
}

if (-not $Pat) {
    Write-Host "請輸入 Coze Personal Access Token (PAT)：" -ForegroundColor Yellow -NoNewline
    $Pat = Read-Host
}

Write-Host "使用 SDK 來源：$SdkSrc" -ForegroundColor Gray

# ==================== Step 2：驗證參數 ====================
if ([string]::IsNullOrWhiteSpace($BotId)) {
    Write-Host "`n❌ Bot ID 不能為空！" -ForegroundColor Red
    exit 1
}

if ([string]::IsNullOrWhiteSpace($Pat)) {
    Write-Host "`n❌ PAT Token 不能為空！" -ForegroundColor Red
    exit 1
}

# ==================== Step 3：設定環境變數 ====================
Write-Host "`n【設定環境變數】" -ForegroundColor Yellow

try {
    $env:COZE_BOT_ID = $BotId
    $env:COZE_PAT = $Pat
    $env:COZE_CHAT_SDK_SRC = $SdkSrc
    
    Write-Host "  ✅ COZE_BOT_ID: $env:COZE_BOT_ID" -ForegroundColor Green
    Write-Host "  ✅ COZE_PAT: $($env:COZE_PAT.Substring(0, [Math]::Min(10, $env:COZE_PAT.Length)))..." -ForegroundColor Green
    Write-Host "  ✅ COZE_CHAT_SDK_SRC: $env:COZE_CHAT_SDK_SRC" -ForegroundColor Green
} catch {
    Write-Host "  ❌ 設定環境變數失敗: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# ==================== Step 4：驗證後端 API ====================
Write-Host "`n【驗證後端 API】" -ForegroundColor Yellow
Write-Host "正在檢查後端是否運行..." -ForegroundColor Gray

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/support/coze/bootstrap" -Method GET -TimeoutSec 5 -ErrorAction Stop
    
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✅ 後端 Bootstrap API 正常運行" -ForegroundColor Green
        
        $jsonResponse = $response.Content | ConvertFrom-Json
        Write-Host "  回應內容:" -ForegroundColor Gray
        Write-Host "    - botId: $($jsonResponse.botId)" -ForegroundColor Gray
        Write-Host "    - token: $($jsonResponse.token.Substring(0, [Math]::Min(10, $jsonResponse.token.Length)))..." -ForegroundColor Gray
        Write-Host "    - sdkSrc: $($jsonResponse.sdkSrc)" -ForegroundColor Gray
        Write-Host "    - expiresIn: $($jsonResponse.expiresIn)" -ForegroundColor Gray
        Write-Host "    - serverTime: $($jsonResponse.serverTime)" -ForegroundColor Gray
    }
} catch {
    Write-Host "  ⚠️  無法連接到後端 API" -ForegroundColor Yellow
    Write-Host "  原因: $($_.Exception.Message)" -ForegroundColor Gray
    Write-Host "`n  請確認：" -ForegroundColor Yellow
    Write-Host "    1. 後端是否已啟動: cd backend && mvn spring-boot:run" -ForegroundColor Gray
    Write-Host "    2. 後端是否運行在 http://localhost:8080" -ForegroundColor Gray
    Write-Host "    3. 防火牆是否阻擋連線" -ForegroundColor Gray
}

# ==================== Step 5：產生設定檔（可選） ====================
Write-Host "`n【產生環境變數設定檔】" -ForegroundColor Yellow
$envFilePath = "coze-env.ps1"

$envFileContent = @"
# Coze Web Chat SDK 環境變數設定
# 產生時間: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
# 用途: 快速設定 Coze 環境變數

`$env:COZE_BOT_ID="$BotId"
`$env:COZE_PAT="$Pat"
`$env:COZE_CHAT_SDK_SRC="$SdkSrc"

Write-Host "✅ Coze 環境變數已設定" -ForegroundColor Green
Write-Host "  - COZE_BOT_ID: `$env:COZE_BOT_ID" -ForegroundColor Gray
Write-Host "  - COZE_PAT: `$(`$env:COZE_PAT.Substring(0, 10))..." -ForegroundColor Gray
Write-Host "  - COZE_CHAT_SDK_SRC: `$env:COZE_CHAT_SDK_SRC" -ForegroundColor Gray
"@

try {
    Set-Content -Path $envFilePath -Value $envFileContent -Encoding UTF8
    Write-Host "  ✅ 設定檔已產生: $envFilePath" -ForegroundColor Green
    Write-Host "  下次可直接執行: .\\$envFilePath" -ForegroundColor Gray
} catch {
    Write-Host "  ⚠️  無法產生設定檔: $($_.Exception.Message)" -ForegroundColor Yellow
}

# ==================== Step 6：總結 ====================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "設定完成！" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "📋 下一步操作：" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. 啟動後端（如果尚未啟動）：" -ForegroundColor Gray
Write-Host "   cd backend" -ForegroundColor Gray
Write-Host "   mvn spring-boot:run" -ForegroundColor Gray
Write-Host ""
Write-Host "2. 啟動前端：" -ForegroundColor Gray
Write-Host "   cd frontend" -ForegroundColor Gray
Write-Host "   npm run dev" -ForegroundColor Gray
Write-Host ""
Write-Host "3. 開啟瀏覽器測試：" -ForegroundColor Gray
Write-Host "   http://localhost:5173/support" -ForegroundColor Gray
Write-Host ""
Write-Host "4. 檢查 Console 是否顯示：" -ForegroundColor Gray
Write-Host "   [Coze] 初始化完成 ✅" -ForegroundColor Gray
Write-Host ""
Write-Host "5. 執行完整診斷：" -ForegroundColor Gray
Write-Host "   .\\diagnose-coze.ps1" -ForegroundColor Gray
Write-Host ""
Write-Host "⚠️  注意：環境變數只在當前 PowerShell 視窗有效" -ForegroundColor Yellow
Write-Host "   若關閉視窗，需要重新執行：.\\$envFilePath" -ForegroundColor Yellow
Write-Host ""
