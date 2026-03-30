# Stablecoin Collateral System - API Test Script
# PowerShell version for Windows

$BaseUrl = "http://localhost:8081"
$TestResults = @()

function Test-API {
    param(
        [string]$TestName,
        [string]$Method,
        [string]$Endpoint,
        [object]$Body = $null
    )
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Test: $TestName" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    try {
        $Url = "$BaseUrl$Endpoint"
        Write-Host "URL: $Url" -ForegroundColor Gray
        
        if ($Method -eq "GET") {
            $Response = Invoke-RestMethod -Uri $Url -Method Get -ContentType "application/json"
        } else {
            $JsonBody = $Body | ConvertTo-Json
            Write-Host "Body: $JsonBody" -ForegroundColor Gray
            $Response = Invoke-RestMethod -Uri $Url -Method $Method -Body $JsonBody -ContentType "application/json"
        }
        
        Write-Host "Response:" -ForegroundColor Green
        $Response | ConvertTo-Json | Write-Host -ForegroundColor Green
        
        $TestResults += @{
            TestName = $TestName
            Status = "PASS"
            Message = "Success"
        }
        
        return $Response
    }
    catch {
        Write-Host "Error: $_" -ForegroundColor Red
        $TestResults += @{
            TestName = $TestName
            Status = "FAIL"
            Message = $_.Exception.Message
        }
        return $null
    }
}

# Main Test Suite
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Stablecoin Collateral Management System - API Test Suite  ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
Write-Host "Base URL: $BaseUrl" -ForegroundColor Yellow
Write-Host "Start Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Yellow
Write-Host ""

# Test 1: Get Dashboard
Test-API -TestName "Test 1: Get Dashboard" `
         -Method "GET" `
         -Endpoint "/api/reserve/dashboard"

# Test 2: Get Asset Portfolio
Test-API -TestName "Test 2: Get Asset Portfolio" `
         -Method "GET" `
         -Endpoint "/api/assets/portfolio"

# Test 3: Deposit Collateral - First Deposit (1000000 USD)
$deposit1Body = @{
    txHash = "0x" + [System.Guid]::NewGuid().ToString().Replace("-","").Substring(0, 64)
    amount = 1000000.000000
    depositedAt = (Get-Date -AsUTC -Format "yyyy-MM-ddTHH:mm:ss")
    operator = "treasury_dept_001"
    remark = "First collateral deposit"
}

Test-API -TestName "Test 3: Deposit Collateral - First Deposit (1000000 USD)" `
         -Method "POST" `
         -Endpoint "/api/collateral/deposit" `
         -Body $deposit1Body

# Test 4: Query Deposits
Test-API -TestName "Test 4: Query Collateral Deposits" `
         -Method "GET" `
         -Endpoint "/api/collateral/deposits"

# Test 5: Check Reserve
Test-API -TestName "Test 5: Check Reserve (Verify 1:1 Coverage)" `
         -Method "POST" `
         -Endpoint "/api/reserve/check?stablecoinSupply=500000"

# Test 6: Trigger Snapshot
Test-API -TestName "Test 6: Trigger Reserve Snapshot" `
         -Method "POST" `
         -Endpoint "/api/reserve/snapshot"

# Test 7: Deposit Collateral - Second Deposit (2000000 USD)
$deposit2Body = @{
    txHash = "0x" + [System.Guid]::NewGuid().ToString().Replace("-","").Substring(0, 64)
    amount = 2000000.000000
    depositedAt = (Get-Date -AsUTC -Format "yyyy-MM-ddTHH:mm:ss")
    operator = "treasury_dept_002"
    remark = "Second collateral deposit"
}

Test-API -TestName "Test 7: Deposit Collateral - Second Deposit (2000000 USD)" `
         -Method "POST" `
         -Endpoint "/api/collateral/deposit" `
         -Body $deposit2Body

# Test 8: Get Reserve History
Test-API -TestName "Test 8: Get Reserve History" `
         -Method "GET" `
         -Endpoint "/api/reserve/history?start=2026-03-28T00:00:00&end=2026-03-29T23:59:59"

# Test 9: Get Risk Alerts
Test-API -TestName "Test 9: Get Risk Alerts" `
         -Method "GET" `
         -Endpoint "/api/risk/alerts"

# Test 10: Generate Audit Report
Test-API -TestName "Test 10: Generate Audit Report" `
         -Method "POST" `
         -Endpoint "/api/audit/reports/generate?start=2026-03-28T00:00:00&end=2026-03-29T23:59:59&operator=test_user"

# Test 11: Get Audit Reports
Test-API -TestName "Test 11: Get Audit Reports" `
         -Method "GET" `
         -Endpoint "/api/audit/reports"

# Test 12: Get Single Asset
Test-API -TestName "Test 12: Get Single Asset (US Treasury)" `
         -Method "GET" `
         -Endpoint "/api/assets/assets/US_TREASURY"

# Summary
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                      Test Summary                          ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$PassCount = ($TestResults | Where-Object { $_.Status -eq "PASS" }).Count
$FailCount = ($TestResults | Where-Object { $_.Status -eq "FAIL" }).Count
$TotalCount = $TestResults.Count

Write-Host "Total Tests: $TotalCount" -ForegroundColor Yellow
Write-Host "Passed: $PassCount" -ForegroundColor Green
Write-Host "Failed: $FailCount" -ForegroundColor Red
Write-Host ""

Write-Host "Test Results:" -ForegroundColor Cyan
$TestResults | ForEach-Object {
    $StatusColor = if ($_.Status -eq "PASS") { "Green" } else { "Red" }
    Write-Host "  [$($_.Status)] $($_.TestName)" -ForegroundColor $StatusColor
}

Write-Host ""
Write-Host "End Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Yellow
Write-Host ""

if ($FailCount -eq 0) {
    Write-Host "✓ All tests passed!" -ForegroundColor Green
} else {
    Write-Host "✗ Some tests failed. Please check the errors above." -ForegroundColor Red
}

Write-Host ""
