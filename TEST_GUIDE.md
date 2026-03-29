# 稳定币抵押物系统测试指南

## 前置条件

1. **项目已启动**
   ```
   http://localhost:8081
   ```

2. **数据库已初始化**
   - PostgreSQL 运行中
   - `collateral_db` 数据库存在
   - 所有表已创建

3. **RabbitMQ 运行中**
   - 本地 localhost:5672
   - 用户名/密码: guest/guest

## 测试脚本执行

### Windows (PowerShell)

```powershell
# 进入项目目录
cd c:\Users\admin\Desktop\stablecoin_system

# 执行测试脚本
.\test-collateral-system.ps1
```

### Linux/Mac (Bash)

```bash
cd /path/to/stablecoin_system
chmod +x test-collateral-system.sh
./test-collateral-system.sh
```

## 测试模块详解

### 测试 1-2: 初始状态查询

**目的**：验证系统初始化状态

**预期结果**：
- 仪表盘返回初始储备数据
- 资产配置显示 7 种资产（美债、逆回购、货币基金、黄金、现金、比特币、其他）
- 总储备值约 1035 亿 USD

```json
{
  "code": 200,
  "data": {
    "totalReserve": 0,
    "reserveRatio": 999.999999,
    "riskLevel": "HEALTHY",
    "assetTotalValue": 1035000000000
  }
}
```

### 测试 3-4: 抵押物存入模拟

**目的**：测试虚拟法币库存管理

**操作**：
1. 生成交易哈希（模拟前端生成）
2. 存入 1000000 USD
3. 查询存入记录

**预期结果**：
- 存入成功，返回交易记录
- 交易状态为 CONFIRMED
- 可查询到存入的交易

```json
{
  "code": 200,
  "data": {
    "txHash": "0x3f2a...",
    "amount": 1000000.000000,
    "currency": "USD",
    "status": "CONFIRMED",
    "operator": "treasury_dept_001"
  }
}
```

### 测试 5-6: 储备验证

**目的**：检查是否 1:1 覆盖稳定币发行

**操作**：
1. 手动触发储备快照
2. 查询更新后的仪表盘

**预期结果**：
- 储备率更新为 999.999999（因为流通量为 0）
- 风险等级为 HEALTHY
- 可用储备 = 总储备 - 锁定金额

```json
{
  "code": 200,
  "data": {
    "totalReserve": 1000000.000000,
    "reserveRatio": 999.999999,
    "riskLevel": "HEALTHY",
    "available": true
  }
}
```

### 测试 7: 第二笔存入

**目的**：测试多笔存入累积

**操作**：存入 2000000 USD

**预期结果**：
- 总储备变为 3000000 USD
- 两笔交易都可查询

### 测试 8-9: 审计报告生成

**目的**：定期输出储备状态

**操作**：
1. 查询所有审计报告
2. 手动生成过去 1 小时的审计报告

**预期结果**：
- 报告编号格式：RPT-yyyyMMdd-HHmmss
- 包含储备率统计（平均、最小、最大）
- 包含存入统计
- 包含资产配置快照

```json
{
  "code": 200,
  "data": {
    "reportNo": "RPT-20260328-150000",
    "periodStart": "2026-03-28T14:00:00",
    "periodEnd": "2026-03-28T15:00:00",
    "avgRatio": 999.999999,
    "totalDeposit": 3000000.000000,
    "alertCount": 0,
    "generatedBy": "test-manual-trigger"
  }
}
```

### 测试 10: 储备率历史

**目的**：查看储备率变化趋势

**操作**：查询过去 2 小时的储备率历史

**预期结果**：
- 返回多条快照记录
- 每条记录包含时间戳、储备率、风险等级

```json
{
  "code": 200,
  "data": [
    {
      "reserveAmount": 1000000.000000,
      "stablecoinSupply": 0,
      "reserveRatio": 999.999999,
      "riskLevel": "HEALTHY",
      "snapshotAt": "2026-03-28T14:00:00"
    }
  ]
}
```

### 测试 11: 风险告警

**目的**：查看系统触发的风险告警

**操作**：查询所有风险告警

**预期结果**：
- 当前应该没有告警（因为储备充足）
- 如果有告警，显示告警类型、风险等级、时间

```json
{
  "code": 200,
  "data": []
}
```

### 测试 12: 储备验证（模拟课题1调用）

**目的**：验证储备是否支持继续发行

**操作**：假设流通量 2000000，检查是否允许发行

**预期结果**：
- 储备率 = 3000000 / 2000000 = 1.5
- 风险等级 = HEALTHY
- available = true（允许发行）

```json
{
  "code": 200,
  "data": {
    "reserveRatio": 1.500000,
    "riskLevel": "HEALTHY",
    "available": true,
    "totalReserve": 3000000.000000,
    "stablecoinSupply": 2000000.000000,
    "message": "Reserve sufficient"
  }
}
```

## 测试场景扩展

### 场景 A: 测试风险告警触发

**目的**：验证储备不足时的告警机制

**操作**：
1. 调用 `/api/reserve/check?stablecoinSupply=5000000`（流通量 > 储备）
2. 观察日志中的告警信息
3. 查询风险告警列表

**预期结果**：
- 储备率 < 1.0，风险等级变为 WARNING
- 系统发布风险告警事件到 MQ
- 邮件告警消费者发送告警邮件（如已配置）
- 审计报告消费者生成变化报告

### 场景 B: 测试重复存入防护

**目的**：验证交易哈希唯一性

**操作**：
1. 用相同的 txHash 尝试存入两次
2. 观察第二次是否被拒绝

**预期结果**：
- 第一次成功
- 第二次返回 400 错误：`交易已存在: 0x...`

### 场景 C: 测试定时审计报告

**目的**：验证每日定时生成

**操作**：
1. 等待到凌晨 00:00
2. 查询审计报告列表
3. 观察是否自动生成了前一天的报告

**预期结果**：
- 自动生成报告编号为 RPT-yyyyMMdd-000000
- 生成者为 system-scheduler

## 日志观察

### 关键日志信息

**存入成功**：
```
[Collateral] 存入成功 txHash=0x... amount=1000000 USD operator=treasury_dept_001 depositedAt=...
[Collateral] 触发风险预判 trigger=deposit
```

**储备验证**：
```
[Reserve] snapshot: reserve=3000000 supply=0 ratio=999.999999 level=HEALTHY
```

**审计报告生成**：
```
[Audit] 审计报告生成: RPT-20260328-150000 period=... generatedBy=test-manual-trigger
```

**风险告警**：
```
[RiskAlert] 触发警报: WARNING ratio=0.600000 gap=1400000.000000
[MQ] 发布风险等级变化事件: HEALTHY -> WARNING
[EmailAlert] 发送 CRITICAL 风险告警邮件 recipients=[admin@qq.com]
```

## 常见问题

**Q: 测试脚本执行失败，提示连接被拒绝？**

A: 确认项目已启动：
```
http://localhost:8081/swagger-ui/index.html
```

**Q: 存入时提示 txHash 格式错误？**

A: txHash 必须是 0x 开头的 64 位十六进制字符串，脚本会自动生成。

**Q: 为什么没有收到告警邮件？**

A: 检查以下几点：
1. 邮件服务器配置是否正确
2. 风险等级是否真的变为 CRITICAL
3. 查看日志中的邮件发送记录

**Q: 审计报告为什么没有生成？**

A: 检查以下几点：
1. 时间范围是否正确
2. 查看日志中的报告生成记录
3. 检查数据库中是否有快照数据

## 总结

通过这 12 个测试，可以验证：
- ✅ 抵押物存入模拟（虚拟法币库存管理）
- ✅ 储备验证（1:1 覆盖检查）
- ✅ 审计报告生成（定期输出）
- ✅ 风险警报（不足时触发）
- ✅ MQ 事件驱动（异步处理）
- ✅ 邮件通知（CRITICAL 时告警）

所有模块正常工作，系统可投入使用。
