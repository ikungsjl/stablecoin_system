# 稳定币抵押物系统集成测试脚本 (PowerShell)
# 测试模块：抵押物存入、储备验证、审计报告、风险警报

$BASE_URL = "http://localhost:8081"

function Generate-TxHash {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RNGCryptoServiceProvider]::new()
    $rng.GetBytes($bytes)
    return "0x" + ($bytes | ForEach-Object { "{0:x2}" -f $_ }) -join ""
}

function Test-API {
    param(
        [string]$TestName,
        [string]$Method,
        [string]$Endpoint,
        [object]$Body
    )
    
    Write-Host ""
    Write-Host "[$TestName]" -ForegroundColor Cyan
    Write-Host "$Method $Endpoint" -ForegroundColor Yellow
    
    $url = "$BASE_URL$Endpoint"
    $params = @{
        Uri = $url
        Method = $Method
        Headers = @{ "Content-Type" = "application/json" }
    }
    
    if ($Body) {
        $params["Body"] = $Body | ConvertTo-Json -Depth 10
    }
    
    try {
        $response = Invoke-RestMethod @params
        $response | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor Green
    } catch {
        Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "==========================================" -ForegroundColor Magenta
Write-Host "稳定币抵押物系统集成测试" -ForegroundColor Magenta
Write-Host "==========================================" -ForegroundColor Magenta

# 测试 1: 查询初始储备状态
Test-API -TestName "测试 1: 查询初始储备状态" `
         -Method "GET" `
         -Endpoint "/api/reserve/dashboard"

# 测试 2: 查询资产配置
Test-API -TestName "测试 2: 查询储备资产配置" `
         -Method "GET" `
         -Endpoint "/api/assets/portfolio"

# 测试 3: 存入抵押物（第一笔）
$txHash1 = Generate-TxHash
$deposit1Body = @{
    txHash = $txHash1
    amount = 1000000.000000
    depositedAt = (Get-Date -AsUTC -Format "yyyy-MM-ddTHH:mm:ss")
    operator = "treasury_dept_001"
    remark = "Initial collateral deposit"
}

Test-API -TestName "测试 3: 存入抵押物 - 第一笔 (1000000 USD)" `
         -Method "POST" `
         -Endpoint "/api/collateral/deposit" `
         -Body $deposit1Body

# 测试 4: 查询存入记录
Test-API -TestName "测试 4: 查询存入记录" `
         -Method "GET" `
         -Endpoint "/api/collateral/deposits"

# 测试 5: 手动触发储备验证快照
Test-API -TestName "测试 5: 手动触发储备验证快照" `
         -Method "POST" `
         -Endpoint "/api/reserve/snapshot"

# 测试 6: 查询更新后的仪表盘
Test-API -TestName "测试 6: 查询更新后的仪表盘（存入后）" `
         -Method "GET" `
         -Endpoint "/api/reserve/dashboard"

# 测试 7: 存入第二笔抵押物
$txHash2 = Generate-TxHash
$deposit2Body = @{
    txHash = $txHash2
    amount = 2000000.000000
    depositedAt = (Get-Date -AsUTC -Format "yyyy-MM-ddTHH:mm:ss")
    operator = "treasury_dept_002"
    remark = "Second collateral deposit"
}

Test-API -TestName "测试 7: 存入抵押物 - 第二笔 (2000000 USD)" `
         -Method "POST" `
         -Endpoint "/api/collateral/deposit" `
         -Body $deposit2Body

# 测试 8: 查询所有审计报告
Test-API -TestName "测试 8: 查询所有审计报告" `
         -Method "GET" `
         -Endpoint "/api/audit/reports"

# 测试 9: 手动生成审计报告
$startTime = (Get-Date -AsUTC).AddHours(-1).ToString("yyyy-MM-ddTHH:mm:ss")
$endTime = (Get-Date -AsUTC).ToString("yyyy-MM-ddTHH:mm:ss")
$auditBody = @{
    start = $startTime
    end = $endTime
    operator = "test-manual-trigger"
}

Test-API -TestName "测试 9: 手动生成审计报告（过去1小时）" `
         -Method "POST" `
         -Endpoint "/api/audit/reports/generate" `
         -Body $auditBody

# 测试 10: 查询储备率历史
$historyStart = (Get-Date -AsUTC).AddHours(-2).ToString("yyyy-MM-ddTHH:mm:ss")
$historyEnd = (Get-Date -AsUTC).ToString("yyyy-MM-ddTHH:mm:ss")

Test-API -TestName "测试 10: 查询储备率历史（过去2小时）" `
         -Method "GET" `
         -Endpoint "/api/reserve/history?start=$historyStart&end=$historyEnd"

# 测试 11: 查询风险告警
Test-API -TestName "测试 11: 查询风险告警" `
         -Method "GET" `
         -Endpoint "/api/risk/alerts"

# 测试 12: 验证储备是否支持发行
Test-API -TestName "测试 12: 验证储备是否支持发行（假设流通量 2000000）" `
         -Method "POST" `
         -Endpoint "/api/reserve/check?stablecoinSupply=2000000"

Write-Host ""
Write-Host "==========================================" -ForegroundColor Magenta
Write-Host "测试完成" -ForegroundColor Magenta
Write-Host "==========================================" -ForegroundColor Magenta
