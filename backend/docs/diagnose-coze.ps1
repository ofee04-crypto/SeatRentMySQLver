# ==================== Coze Web Chat SDK 診斷工具 v2.0 ====================
# 用途：檢查環境變數、後端服務、前端配置、網路連通性
# 版本：v2.0（新增 API 連通性與 502/TLB 診斷）

param(
    [switch]$Verbose,
    [switch]$SkipNetwork
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Coze Web Chat SDK 診斷工具 v2.0" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$errorCount = 0
$warningCount = 0

# ==================== 1. 檢查環境變數 ====================
Write-Host "【1】檢查環境變數..." -ForegroundColor Yellow

if ($env:COZE_BOT_ID) {
    Write-Host "  ✅ COZE_BOT_ID: $env:COZE_BOT_ID" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  COZE_BOT_ID 未設定（將使用 application.yml 預設值）" -ForegroundColor Yellow
    $warningCount++
}

if ($env:COZE_PAT) {
    $maskedPat = $env:COZE_PAT.Substring(0, [Math]::Min(10, $env:COZE_PAT.Length)) + "..."
    Write-Host "  ✅ COZE_PAT: $maskedPat" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  COZE_PAT 未設定（將使用 application.yml 預設值）" -ForegroundColor Yellow
    $warningCount++
}

if ($env:COZE_CHAT_SDK_SRC) {
    Write-Host "  ✅ COZE_CHAT_SDK_SRC: $env:COZE_CHAT_SDK_SRC" -ForegroundColor Green
} else {
    Write-Host "  ℹ️  COZE_CHAT_SDK_SRC 未設定（前端將使用 npm import）" -ForegroundColor Gray
}

# ==================== 2. 檢查後端服務 ====================
Write-Host "`n【2】檢查後端服務..." -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/support/coze/bootstrap" -Method GET -TimeoutSec 5 -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✅ Bootstrap API 正常運行 (200 OK)" -ForegroundColor Green
        
        $json = $response.Content | ConvertFrom-Json
        Write-Host "  ├─ botId: $($json.botId)" -ForegroundColor Gray
        Write-Host "  ├─ platformHint: $($json.platformHint)" -ForegroundColor Gray
        Write-Host "  ├─ apiHostHint: $($json.apiHostHint)" -ForegroundColor Gray
        Write-Host "  └─ sdkMode: $($json.sdkMode)" -ForegroundColor Gray
    }
} catch {
    $statusCode = $null
    if ($_.Exception.Response) {
        $statusCode = [int]$_.Exception.Response.StatusCode
    }
    
    if ($statusCode -eq 400) {
        Write-Host "  ❌ Bootstrap API 回傳 400 Bad Request（配置不完整）" -ForegroundColor Red
        $errorCount++
    } elseif ($statusCode -eq 404) {
        Write-Host "  ❌ Bootstrap API 回傳 404 Not Found（Controller 路徑錯誤）" -ForegroundColor Red
        $errorCount++
    } else {
        Write-Host "  ❌ 後端服務無法連接" -ForegroundColor Red
        Write-Host "     錯誤: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "     請確認後端是否啟動: cd backend && mvn spring-boot:run" -ForegroundColor Yellow
        $errorCount++
    }
}

# ==================== 3. 網路連通性測試（新增） ====================
if (-not $SkipNetwork) {
    Write-Host "`n【3】檢查 Coze API 連通性..." -ForegroundColor Yellow
    
    $apiHosts = @("api.coze.com", "lf-cdn.coze.com", "lf-cdn.coze.cn")
    
    foreach ($h in $apiHosts) {
        Write-Host "  測試 $h..." -ForegroundColor Gray
        
        # DNS 解析測試
        try {
            $dns = Resolve-DnsName -Name $h -ErrorAction Stop
            $ipAddr = ($dns | Where-Object { $_.Type -eq 'A' } | Select-Object -First 1).IPAddress
            if ($ipAddr) {
                Write-Host "    ├─ DNS: ✅ 解析成功 ($ipAddr)" -ForegroundColor Green
            } else {
                Write-Host "    ├─ DNS: ✅ 解析成功 (CNAME)" -ForegroundColor Green
            }
        } catch {
            Write-Host "    ├─ DNS: ❌ 解析失敗（NXDOMAIN 或網路問題）" -ForegroundColor Red
            Write-Host "       提示：可能被公司網路/VPN 擋住，嘗試手機熱點" -ForegroundColor Yellow
            $warningCount++
            continue
        }
        
        # HTTPS 連線測試
        try {
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $asyncResult = $tcpClient.BeginConnect($h, 443, $null, $null)
            $waitHandle = $asyncResult.AsyncWaitHandle
            
            if ($waitHandle.WaitOne(3000, $false)) {
                $tcpClient.EndConnect($asyncResult)
                Write-Host "    └─ TCP 443: ✅ 可連線" -ForegroundColor Green
            } else {
                Write-Host "    └─ TCP 443: ⚠️ 連線逾時" -ForegroundColor Yellow
                $warningCount++
            }
            $tcpClient.Close()
        } catch {
            Write-Host "    └─ TCP 443: ❌ 連線失敗" -ForegroundColor Red
            $warningCount++
        }
    }
} else {
    Write-Host "`n【3】跳過網路連通性測試（使用 -SkipNetwork 參數）" -ForegroundColor Gray
}

# ==================== 4. 前端配置檢查 ====================
Write-Host "`n【4】檢查前端配置..." -ForegroundColor Yellow

# 檢查 http.js
$httpJsPath = "frontend/src/api/http.js"
if (Test-Path $httpJsPath) {
    $httpJsContent = Get-Content $httpJsPath -Raw
    if ($httpJsContent -match "baseURL:\s*['""]\/api['""]") {
        Write-Host "  ✅ http.js baseURL 配置正確: '/api'" -ForegroundColor Green
    } else {
        Write-Host "  ❌ http.js baseURL 配置錯誤" -ForegroundColor Red
        Write-Host "     應該是: baseURL: '/api'" -ForegroundColor Yellow
        $errorCount++
    }
} else {
    Write-Host "  ❌ 找不到 $httpJsPath" -ForegroundColor Red
    $errorCount++
}

# 檢查 support.js
$supportJsPath = "frontend/src/api/modules/support.js"
if (Test-Path $supportJsPath) {
    $supportJsContent = Get-Content $supportJsPath -Raw
    if ($supportJsContent -match "http\.get\(['""]\/support\/coze\/bootstrap['""]") {
        Write-Host "  ✅ support.js 路徑配置正確: '/support/coze/bootstrap'" -ForegroundColor Green
    } elseif ($supportJsContent -match "http\.get\(['""]\/api\/support\/coze\/bootstrap['""]") {
        Write-Host "  ❌ support.js 路徑配置錯誤（不應該有 /api 前綴）" -ForegroundColor Red
        Write-Host "     應該是: http.get('/support/coze/bootstrap')" -ForegroundColor Yellow
        $errorCount++
    } else {
        Write-Host "  ❌ support.js 路徑配置錯誤" -ForegroundColor Red
        $errorCount++
    }
} else {
    Write-Host "  ❌ 找不到 $supportJsPath" -ForegroundColor Red
    $errorCount++
}

# 檢查 useCozeChat composable
$composablePath = "frontend/src/composables/useCozeChat.js"
if (Test-Path $composablePath) {
    Write-Host "  ✅ useCozeChat.js 存在（支援重試/降級機制）" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  找不到 useCozeChat.js（建議新增以支援重試/降級）" -ForegroundColor Yellow
    $warningCount++
}

# 檢查 vite.config.js
$viteConfigPath = "frontend/vite.config.js"
if (Test-Path $viteConfigPath) {
    $viteConfigContent = Get-Content $viteConfigPath -Raw
    if ($viteConfigContent -match "['""]\/api['""]:\s*\{[^}]*target:\s*['""]http:\/\/localhost:8080['""]") {
        Write-Host "  ✅ vite.config.js proxy 配置正確" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  vite.config.js proxy 配置可能需要檢查" -ForegroundColor Yellow
        $warningCount++
    }
} else {
    Write-Host "  ❌ 找不到 $viteConfigPath" -ForegroundColor Red
    $errorCount++
}

# 檢查 SupportCenterView.vue
$supportViewPath = "frontend/src/views/support/SupportCenterView.vue"
if (Test-Path $supportViewPath) {
    $supportViewContent = Get-Content $supportViewPath -Raw
    if ($supportViewContent -match "useCozeChat") {
        Write-Host "  ✅ SupportCenterView.vue 使用 useCozeChat composable" -ForegroundColor Green
    } elseif ($supportViewContent -match "const\s+initCozeChat\s*=\s*async\s*\(\)") {
        Write-Host "  ⚠️  SupportCenterView.vue 使用舊版初始化方式" -ForegroundColor Yellow
        $warningCount++
    } else {
        Write-Host "  ❌ SupportCenterView.vue 缺少 Coze 初始化邏輯" -ForegroundColor Red
        $errorCount++
    }
} else {
    Write-Host "  ❌ 找不到 $supportViewPath" -ForegroundColor Red
    $errorCount++
}

# ==================== 5. npm 套件檢查 ====================
Write-Host "`n【5】檢查 npm 套件..." -ForegroundColor Yellow

$packageJsonPath = "frontend/package.json"
if (Test-Path $packageJsonPath) {
    $packageJson = Get-Content $packageJsonPath -Raw | ConvertFrom-Json
    if ($packageJson.dependencies.'@coze/chat-sdk') {
        Write-Host "  ✅ @coze/chat-sdk 已安裝: $($packageJson.dependencies.'@coze/chat-sdk')" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  @coze/chat-sdk 未在 dependencies 中" -ForegroundColor Yellow
        Write-Host "     執行: cd frontend && npm install @coze/chat-sdk" -ForegroundColor Yellow
        $warningCount++
    }
} else {
    Write-Host "  ❌ 找不到 $packageJsonPath" -ForegroundColor Red
    $errorCount++
}

# ==================== 6. 路由配置檢查 ====================
Write-Host "`n【6】檢查路由配置..." -ForegroundColor Yellow
$routerPath = "frontend/src/router/index.js"
if (Test-Path $routerPath) {
    $routerContent = Get-Content $routerPath -Raw
    $supportRouteExists = $routerContent -match "path:\s*['""]\/support['""]"
    $reportRouteExists = $routerContent -match "path:\s*['""]\/support\/report['""]"
    
    if ($supportRouteExists -and $reportRouteExists) {
        Write-Host "  ✅ 路由配置正確（/support, /support/report）" -ForegroundColor Green
    } else {
        if (-not $supportRouteExists) {
            Write-Host "  ❌ 缺少 /support 路由" -ForegroundColor Red
            $errorCount++
        }
        if (-not $reportRouteExists) {
            Write-Host "  ⚠️  缺少 /support/report 路由" -ForegroundColor Yellow
            $warningCount++
        }
    }
} else {
    Write-Host "  ❌ 找不到 $routerPath" -ForegroundColor Red
    $errorCount++
}

# ==================== 診斷總結 ====================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "診斷總結" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($errorCount -eq 0 -and $warningCount -eq 0) {
    Write-Host "✅ 所有檢查通過！" -ForegroundColor Green
} else {
    if ($errorCount -gt 0) {
        Write-Host "❌ 發現 $errorCount 個錯誤" -ForegroundColor Red
    }
    if ($warningCount -gt 0) {
        Write-Host "⚠️  發現 $warningCount 個警告" -ForegroundColor Yellow
    }
}

# ==================== 502/TLB 錯誤診斷建議 ====================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "502/TLB 錯誤診斷建議" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host @"
若點擊泡泡後出現 502 Bad Gateway / server: TLB：

1. 檢查 Network Tab 中 chatapp 請求的 Response Headers
   - 記錄 x-akamai-request-id（用於向 Coze 回報）

2. 嘗試切換網路
   - 使用手機熱點測試（排除公司網路問題）
   - 使用 VPN 測試

3. 確認 Coze 服務狀態
   - 訪問 https://status.coze.com（若有）
   - 等待 5-10 分鐘後重試

4. 前端降級處理
   - 確認 useCozeChat.js 已實作重試機制
   - 確認 SupportCenterView.vue 顯示降級 UI
"@ -ForegroundColor Gray

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "快速修正指南" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host @"
1. 安裝 npm 套件（如果尚未安裝）：
   cd frontend && npm install @coze/chat-sdk

2. 啟動後端：
   cd backend && mvn spring-boot:run

3. 啟動前端：
   cd frontend && npm run dev

4. 測試頁面：
   http://localhost:5173/support
"@ -ForegroundColor Gray

Write-Host "`n診斷完成！" -ForegroundColor Cyan
