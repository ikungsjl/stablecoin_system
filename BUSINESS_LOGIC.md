# 稳定币抵押物管理系统（课题2）- 业务逻辑文档

## 目录

1. [系统概述](#系统概述)
2. [核心业务流程](#核心业务流程)
3. [各接口业务逻辑](#各接口业务逻辑)
4. [数据流转](#数据流转)
5. [风险管理机制](#风险管理机制)
6. [事件驱动架构](#事件驱动架构)

---

## 系统概述

### 系统定位

稳定币抵押物管理系统（课题2）是稳定币系统的**储备守门人**，负责：

1. **抵押物管理**：记录和追踪法币存入
2. **储备验证**：确保 1:1 覆盖稳定币发行
3. **风险监控**：实时监测储备充足性
4. **审计追踪**：生成完整的审计报告

### 核心原则

- **1:1 覆盖**：1 USD 存入 = 支撑 1 USDT 发行
- **实时监控**：每 60 秒检查一次储备率
- **主动通知**：风险变化时主动通知课题1
- **完整审计**：所有操作都有记录和追踪

---

## 核心业务流程

### 流程 1：抵押物存入流程

```
前端课题（课题1）
    ↓
生成交易哈希 + 时间戳
    ↓
调用课题2 API：POST /api/collateral/deposit
{
  "txHash": "0x...",
  "amount": 1000000,
  "depositedAt": "2026-03-28T15:04:02",
  "operator": "treasury_dept",
  "remark": "..."
}
    ↓
课题2 处理
├── 1. 验证 txHash 唯一性
│   └── 如果重复 → 返回 400 错误
│
├── 2. 保存存入记录
│   └── INSERT INTO collateral_deposits
│
├── 3. 更新储备池
│   └── UPDATE reserve_pool SET total_usd_amount += amount
│
├── 4. 触发风险预判
│   └── 调用 checkAndSnapshot()
│
└── 5. 返回成功响应
    └── 返回存入记录详情
```

**关键点**：
- txHash 必须唯一（防止重复存入）
- 存入金额直接增加储备池
- 存入后立即检查风险等级
- 如果风险变化，自动通知课题1

---

### 流程 2：储备验证流程

```
课题1 请求发行稳定币
    ↓
调用课题2 API：POST /api/reserve/check?stablecoinSupply=XXX
    ↓
课题2 处理
├── 1. 获取当前储备金额
│   └── SELECT total_usd_amount FROM reserve_pool
│
├── 2. 计算储备率
│   └── ratio = 储备 / 流通量
│
├── 3. 评估风险等级
│   ├── ratio ≥ 1.10 → HEALTHY
│   ├── 1.00 ≤ ratio < 1.10 → NORMAL
│   ├── 0.90 ≤ ratio < 1.00 → WARNING
│   └── ratio < 0.90 → CRITICAL
│
├── 4. 判断是否允许发行
│   ├── HEALTHY/NORMAL → available = true
│   └── WARNING/CRITICAL → available = false
│
└── 5. 返回验证结果
    └── { available, riskLevel, reserveRatio, ... }
        ↓
    课题1 根据 available 决定是否发行
```

**关键点**：
- 储备率 = 总储备 / 流通量
- 只有 available = true 时才允许发行
- 风险等级变化时自动通知课题1

---

### 流程 3：风险监控流程

```
定时任务（每 60 秒）
    ↓
执行 checkAndSnapshot()
    ↓
├── 1. 获取当前储备和流通量
│
├── 2. 计算储备率和风险等级
│
├── 3. 保存快照记录
│   └── INSERT INTO reserve_snapshots
│
├── 4. 检测风险等级变化
│   ├── 如果 HEALTHY → WARNING
│   │   └── 发布风险告警事件
│   │       ├── 保存告警记录
│   │       ├── 发布到 MQ
│   │       ├── 调用课题1 API 暂停发行
│   │       └── 触发审计报告生成
│   │
│   ├── 如果 WARNING → HEALTHY
│   │   └── 发布风险恢复事件
│   │       ├── 发布到 MQ
│   │       ├── 调用课题1 API 恢复发行
│   │       └── 触发审计报告生成
│   │
│   └── 如果无变化
│       └── 仅保存快照，不通知
│
└── 5. 返回快照记录
```

**关键点**：
- 定时检查确保实时监控
- 风险变化时立即通知课题1
- 所有快照都被记录用于审计
- MQ 事件驱动异步处理

---

### 流程 4：审计报告生成流程

```
触发源
├── 1. 定时任务（每天凌晨 00:00）
│   └── 生成前一天的审计报告
│
├── 2. 风险等级变化事件
│   └── MQ 消费者异步生成报告
│
└── 3. 手动触发
    └── 通过 API 手动生成指定时间范围的报告

    ↓
生成审计报告
├── 1. 收集数据
│   ├── 储备率统计（平均、最小、最大）
│   ├── 存入统计（总金额、笔数）
│   ├── 风险告警统计
│   ├── 资产配置快照
│   └── 储备快照
│
├── 2. 生成报告编号
│   └── RPT-yyyyMMdd-HHmmss
│
├── 3. 构建详细报告数据
│   └── JSON 格式存储
│
├── 4. 保存报告
│   └── INSERT INTO audit_reports
│
└── 5. 返回报告详情
```

**关键点**：
- 报告编号唯一，便于追踪
- 包含完整的统计数据
- 支持多种触发方式
- 所有报告都可查询和审计

---

## 各接口业务逻辑

### 接口 1：存入抵押物 - POST /api/collateral/deposit

**业务逻辑**：

```
输入：txHash, amount, depositedAt, operator, remark

步骤 1：验证 txHash 唯一性
  SELECT COUNT(*) FROM collateral_deposits WHERE tx_hash = ?
  IF count > 0 THEN
    RETURN 400 "交易已存在"
  END IF

步骤 2：创建存入记录
  INSERT INTO collateral_deposits (
    tx_hash, amount, currency, usd_amount, exchange_rate,
    operator, status, remark, deposited_at, created_at, updated_at
  ) VALUES (
    txHash, amount, 'USD', amount, 1.0,
    operator, 'CONFIRMED', remark, depositedAt, NOW(), NOW()
  )

步骤 3：更新储备池
  UPDATE reserve_pool
  SET total_usd_amount = total_usd_amount + amount,
      last_updated = NOW()
  WHERE id = 1

步骤 4：触发风险预判
  CALL checkAndSnapshot()
  IF 风险等级变化 THEN
    发布风险变化事件到 MQ
    调用课题1 API 通知
  END IF

步骤 5：返回成功响应
  RETURN 200 { 存入记录详情 }
```

**数据变化**：
- `collateral_deposits` 表：新增 1 条记录
- `reserve_pool` 表：total_usd_amount 增加
- `reserve_snapshots` 表：新增 1 条快照
- 可能触发 `risk_alerts` 表新增告警

---

### 接口 2：查询存入记录 - GET /api/collateral/deposits

**业务逻辑**：

```
输入：start (可选), end (可选)

IF start 和 end 都提供 THEN
  SELECT * FROM collateral_deposits
  WHERE deposited_at BETWEEN start AND end
  ORDER BY deposited_at DESC
ELSE
  SELECT * FROM collateral_deposits
  ORDER BY deposited_at DESC
END IF

RETURN 200 { 记录列表 }
```

**特点**：
- 支持时间范围查询
- 按时间倒序排列
- 用于查看历史存入记录

---

### 接口 3：查询仪表盘 - GET /api/reserve/dashboard

**业务逻辑**：

```
步骤 1：获取储备池数据
  SELECT * FROM reserve_pool WHERE id = 1

步骤 2：获取流通量（从缓存或课题1）
  supply = cachedSupply 或 fetchFromIssuanceService()

步骤 3：计算储备率
  IF supply == 0 THEN
    ratio = 999.999999
  ELSE
    ratio = totalReserve / supply
  END IF

步骤 4：评估风险等级
  riskLevel = evaluateRiskLevel(ratio)

步骤 5：统计活跃告警
  activeAlertCount = SELECT COUNT(*) FROM risk_alerts
                     WHERE status = 'ACTIVE'

步骤 6：计算资产占比
  lowRiskRatio = SUM(usdValue WHERE riskLevel = 1) / totalAsset
  mediumRiskRatio = SUM(usdValue WHERE riskLevel = 2) / totalAsset
  highRiskRatio = SUM(usdValue WHERE riskLevel = 3) / totalAsset

步骤 7：计算年化收益
  estimatedAnnualIncome = SUM(usdValue * annualYield)

步骤 8：返回仪表盘数据
  RETURN 200 { 所有统计数据 }
```

**特点**：
- 实时计算，无缓存
- 包含完整的风险和资产信息
- 用于前端展示

---

### 接口 4：验证储备 - POST /api/reserve/check

**业务逻辑**：

```
输入：stablecoinSupply

步骤 1：获取当前储备
  reserve = SELECT total_usd_amount FROM reserve_pool

步骤 2：计算储备率
  IF stablecoinSupply == 0 THEN
    ratio = 999.999999
  ELSE
    ratio = reserve / stablecoinSupply
  END IF

步骤 3：评估风险等级
  IF ratio >= 1.10 THEN
    riskLevel = 'HEALTHY'
  ELSE IF ratio >= 1.00 THEN
    riskLevel = 'NORMAL'
  ELSE IF ratio >= 0.90 THEN
    riskLevel = 'WARNING'
  ELSE
    riskLevel = 'CRITICAL'
  END IF

步骤 4：判断是否允许发行
  available = (riskLevel == 'HEALTHY' OR riskLevel == 'NORMAL')

步骤 5：返回验证结果
  RETURN 200 {
    reserveRatio: ratio,
    riskLevel: riskLevel,
    available: available,
    totalReserve: reserve,
    stablecoinSupply: stablecoinSupply,
    message: available ? "Reserve sufficient" : "Reserve insufficient"
  }
```

**关键点**：
- 这是课题1 发行前的必要检查
- available = false 时课题1 必须暂停发行
- 不修改任何数据，仅查询和计算

---

### 接口 5：手动触发快照 - POST /api/reserve/snapshot

**业务逻辑**：

```
步骤 1：获取储备和流通量
  reserve = SELECT total_usd_amount FROM reserve_pool
  supply = fetchStablecoinSupply()

步骤 2：计算储备率和风险等级
  ratio = reserve / supply (或 999.999999 if supply == 0)
  riskLevel = evaluateRiskLevel(ratio)

步骤 3：保存快照
  INSERT INTO reserve_snapshots (
    reserve_amount, stablecoin_supply, reserve_ratio,
    risk_level, snapshot_at
  ) VALUES (
    reserve, supply, ratio, riskLevel, NOW()
  )

步骤 4：检测风险变化
  previousRiskLevel = lastRiskLevel
  IF riskLevel != previousRiskLevel THEN
    发布风险变化事件
    调用课题1 API
  END IF

步骤 5：返回快照
  RETURN 200 { 快照详情 }
```

**特点**：
- 通常由定时任务自动执行
- 也可手动触发用于测试
- 每次都会检测风险变化

---

### 接口 6：查询储备率历史 - GET /api/reserve/history

**业务逻辑**：

```
输入：start, end

步骤 1：查询快照记录
  SELECT * FROM reserve_snapshots
  WHERE snapshot_at BETWEEN start AND end
  ORDER BY snapshot_at ASC

步骤 2：返回历史数据
  RETURN 200 { 快照列表 }
```

**特点**：
- 用于绘制储备率趋势图
- 按时间升序排列
- 包含完整的历史数据

---

### 接口 7：生成审计报告 - POST /api/audit/reports/generate

**业务逻辑**：

```
输入：start, end, operator

步骤 1：收集统计数据
  avgRatio = SELECT AVG(reserve_ratio) FROM reserve_snapshots
             WHERE snapshot_at BETWEEN start AND end
  minRatio = SELECT MIN(reserve_ratio) FROM reserve_snapshots
             WHERE snapshot_at BETWEEN start AND end
  maxRatio = SELECT MAX(reserve_ratio) FROM reserve_snapshots
             WHERE snapshot_at BETWEEN start AND end
  totalDeposit = SELECT SUM(usd_amount) FROM collateral_deposits
                 WHERE deposited_at BETWEEN start AND end
                 AND status = 'CONFIRMED'
  alertCount = SELECT COUNT(*) FROM risk_alerts
               WHERE created_at BETWEEN start AND end

步骤 2：生成报告编号
  reportNo = 'RPT-' + NOW().format('yyyyMMdd-HHmmss')

步骤 3：构建详细报告数据
  reportData = {
    periodStart: start,
    periodEnd: end,
    generatedAt: NOW(),
    reserveRatioStats: { average, minimum, maximum },
    depositStats: { totalAmount, count },
    alertStats: { totalCount, riskLevels },
    assetSnapshot: { totalValue, riskRatios, annualIncome },
    finalReserveSnapshot: { ... }
  }

步骤 4：保存报告
  INSERT INTO audit_reports (
    report_no, period_start, period_end,
    avg_ratio, min_ratio, max_ratio,
    total_deposit, alert_count,
    report_data, generated_by, generated_at
  ) VALUES (
    reportNo, start, end,
    avgRatio, minRatio, maxRatio,
    totalDeposit, alertCount,
    reportData, operator, NOW()
  )

步骤 5：返回报告
  RETURN 200 { 报告详情 }
```

**特点**：
- 支持多种触发方式
- 包含完整的统计数据
- 所有数据都可追踪

---

### 接口 8：获取资产组合 - GET /api/assets/portfolio

**业务逻辑**：

```
步骤 1：获取总储备
  totalReserve = SELECT total_usd_amount FROM reserve_pool

步骤 2：查询所有资产
  assets = SELECT * FROM reserve_assets

步骤 3：对每个资产计算实际占比
  FOR EACH asset IN assets DO
    actualRatio = asset.usdValue / totalReserve
    estimatedIncome = asset.usdValue * asset.annualYield
    riskLevelName = getRiskLevelName(asset.riskLevel)
    
    APPEND {
      assetType, assetName, usdValue,
      targetAllocationRatio, actualAllocationRatio,
      riskLevel, riskLevelName,
      annualYield, estimatedAnnualIncome,
      description
    }
  END FOR

步骤 4：返回资产组合
  RETURN 200 { 资产列表 }
```

**关键点**：
- 占比是动态计算的，不存储在数据库
- 当储备变化时，占比自动更新
- 风险等级用数字表示（1/2/3）

---

## 数据流转

### 数据流向图

```
前端课题（课题1）
    ↓
存入请求
    ↓
课题2 处理
├── collateral_deposits（存入记录）
├── reserve_pool（储备池）
├── reserve_snapshots（快照）
├── risk_alerts（告警）
├── audit_reports（审计报告）
└── reserve_assets（资产配置）
    ↓
MQ 事件
├── 风险告警事件 → AuditReportEventConsumer → 生成审计报告
├── 风险告警事件 → EmailAlertEventConsumer → 发送邮件
└── 风险变化事件 → 其他消费者
    ↓
课题1 通知
├── 暂停发行（POST /api/issuance/suspend）
└── 恢复发行（POST /api/issuance/resume）
```

### 表之间的关系

```
collateral_deposits
  ↓ (存入金额)
reserve_pool
  ↓ (定时检查)
reserve_snapshots
  ↓ (风险变化)
risk_alerts
  ↓ (生成报告)
audit_reports

reserve_assets
  ↓ (资产配置)
reserve_pool (计算占比)
```

---

## 风险管理机制

### 风险等级评估

```
储备率范围        风险等级    发行状态    通知课题1
≥ 1.10           HEALTHY     ✅ 允许     无需通知
1.00 ~ 1.10      NORMAL      ✅ 允许     无需通知
0.90 ~ 1.00      WARNING     ❌ 暂停     暂停发行
< 0.90           CRITICAL    ❌ 暂停     暂停发行
```

### 风险变化处理

```
风险等级变化检测
    ↓
IF 变化 THEN
  ├── 1. 保存告警记录
  │   └── INSERT INTO risk_alerts
  │
  ├── 2. 发布到 MQ
  │   └── eventPublisher.publishRiskLevelChange()
  │
  ├── 3. 调用课题1 API
  │   ├── IF 升级 THEN POST /api/issuance/suspend
  │   └── IF 降级 THEN POST /api/issuance/resume
  │
  └── 4. 触发审计报告生成
      └── MQ 消费者异步处理
ELSE
  └── 仅保存快照，不通知
END IF
```

---

## 事件驱动架构

### MQ 事件流

```
风险等级变化事件
    ↓
collateral.events exchange
    ├── routing key: risk.level.change
    │   ↓
    │   queue.risk.level.change
    │   ├── AuditReportEventConsumer
    │   │   └── 生成审计报告
    │   │
    │   └── EmailAlertEventConsumer
    │       └── 发送告警邮件
    │
    └── routing key: reserve.risk.alert
        ↓
        queue.reserve.risk.alert
        └── 其他消费者（预留）
```

### 事件消费者

**1. AuditReportEventConsumer**
- 监听：`queue.risk.level.change`
- 动作：生成过去 1 小时的审计报告
- 特点：异步处理，不影响主流程

**2. EmailAlertEventConsumer**
- 监听：`queue.risk.level.change`
- 动作：当风险等级变为 CRITICAL 时发送邮件
- 特点：异步处理，邮件发送失败不影响系统

### 事件格式

```json
{
  "eventType": "RISK_LEVEL_CHANGE",
  "fromLevel": "HEALTHY",
  "toLevel": "WARNING",
  "eventTime": "2026-03-28T15:04:02",
  "timestamp": 1711612442000
}
```

---

## 定时任务

### 储备检查定时任务

```
触发时间：每 60 秒
执行内容：
  1. 获取当前储备和流通量
  2. 计算储备率
  3. 保存快照
  4. 检测风险变化
  5. 如有变化，发布事件并通知课题1

日志：[Reserve] snapshot: reserve=... supply=... ratio=... level=...
```

### 审计报告定时任务

```
触发时间：每天凌晨 00:00
执行内容：
  1. 生成前一天的审计报告
  2. 时间范围：前一天 00:00:00 ~ 23:59:59
  3. 生成者：system-scheduler

日志：[AuditScheduler] 每日审计报告生成完成
```

---

## 错误处理

### 常见错误场景

**1. 重复存入**
```
错误：交易已存在
原因：txHash 重复
处理：返回 400，拒绝存入
```

**2. 储备不足**
```
错误：储备不足，无法发行
原因：储备率 < 1.0
处理：课题1 暂停发行
```

**3. 课题1 未启动**
```
错误：通知课题1 失败
原因：课题1 服务不可用
处理：记录警告，不影响课题2 运行
```

**4. MQ 消费失败**
```
错误：消费者处理失败
原因：邮件发送失败等
处理：自动重试，最多 3 次
```

---

## 性能考虑

### 查询优化

```
索引：
- collateral_deposits: idx_deposited_at, idx_status
- reserve_snapshots: idx_snapshot_at, idx_risk_level
- risk_alerts: idx_status, idx_created_at
- audit_reports: idx_period
```

### 缓存策略

```
缓存项：
- 稳定币流通量（AtomicReference）
- 上一次风险等级（AtomicReference）

更新频率：
- 流通量：每次检查时更新
- 风险等级：每次快照时更新
```

---

## 文档版本

- 版本：1.0
- 最后更新：2026-03-28
- 系统：稳定币抵押物管理系统（课题2）
- 作者：系统设计团队
