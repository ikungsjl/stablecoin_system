# Swagger 测试用例文档

> 服务地址：`http://localhost:8081/swagger-ui/index.html`  
> 所有接口 Base URL：`http://localhost:8081`  
> 时间格式：`yyyy-MM-ddTHH:mm:ss`（Swagger 中直接填入，**不加引号**）

---

## 目录

1. [抵押物管理 `/api/collateral`](#一抵押物管理)
2. [储备管理 `/api/reserve`](#二储备管理)
3. [储备资产 `/api/assets`](#三储备资产)
4. [风险警报 `/api/alerts`](#四风险警报)
5. [审计报告 `/api/audit`](#五审计报告)
6. [推荐执行顺序](#六推荐执行顺序)

---

## 一、抵押物管理

### 接口 1：POST `/api/collateral/deposit` — 抵押物存入

#### ✅ 用例 1-1：正常存入（标准金额）

**说明**：首笔正常存入，金额 100 万 USD，验证返回 CONFIRMED 状态。

**Request Body**：
```json
{
  "txHash": "0xaabbccddee1122334455667788990011aabbccddee1122334455667788990011",
  "amount": 1000000.000000,
  "depositedAt": "2026-03-30T09:00:00",
  "operator": "treasury_dept_001",
  "remark": "首笔抵押物存入"
}
```

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "txHash": "0xaabbccddee1122334455667788990011aabbccddee1122334455667788990011",
    "amount": 1000000.000000,
    "currency": "USD",
    "usdAmount": 1000000.000000,
    "exchangeRate": 1.000000,
    "operator": "treasury_dept_001",
    "status": "CONFIRMED",
    "remark": "首笔抵押物存入"
  }
}
```

**验证要点**：`code`=200，`status`="CONFIRMED"，`currency`="USD"，`exchangeRate`=1.000000，返回 txHash 与请求一致

---

#### ✅ 用例 1-2：正常存入（大额金额）

**说明**：大额存入 5000 万 USD，验证大数精度不丢失。

**Request Body**：
```json
{
  "txHash": "0x1111111111111111111111111111111111111111111111111111111111111111",
  "amount": 50000000.000000,
  "depositedAt": "2026-03-30T10:00:00",
  "operator": "treasury_dept_002",
  "remark": "大额抵押物存入"
}
```

**预期响应**：`code`=200，`amount`=50000000.000000（精度保持），`status`="CONFIRMED"

---

#### ✅ 用例 1-3：正常存入（最小合法金额）

**说明**：测试边界最小金额 0.000001，验证阈值校验通过。

**Request Body**：
```json
{
  "txHash": "0x2222222222222222222222222222222222222222222222222222222222222222",
  "amount": 0.000001,
  "depositedAt": "2026-03-30T10:30:00",
  "operator": "test_operator",
  "remark": "最小金额测试"
}
```

**预期响应**：`code`=200，`amount`=0.000001，`status`="CONFIRMED"

---

#### ✅ 用例 1-4：正常存入（不带 remark 可选字段）

**说明**：remark 为可选字段，不传时应正常存入。

**Request Body**：
```json
{
  "txHash": "0x3333333333333333333333333333333333333333333333333333333333333333",
  "amount": 500000.000000,
  "depositedAt": "2026-03-30T11:00:00",
  "operator": "auto_system"
}
```

**预期响应**：`code`=200，`remark`=null 或字段缺省

---

#### ✅ 用例 1-5：正常存入（未来时间戳）

**说明**：depositedAt 为未来时间，验证系统不做时间限制。

**Request Body**：
```json
{
  "txHash": "0x9999999999999999999999999999999999999999999999999999999999999999",
  "amount": 200000.000000,
  "depositedAt": "2026-12-31T23:59:59",
  "operator": "future_op",
  "remark": "未来时间测试"
}
```

**预期响应**：`code`=200，`depositedAt` 与请求一致

---

#### ❌ 用例 1-6：重复交易哈希（幂等校验）

**说明**：使用用例 1-1 中相同的 txHash 再次存入，应被拒绝。

**Request Body**：
```json
{
  "txHash": "0xaabbccddee1122334455667788990011aabbccddee1122334455667788990011",
  "amount": 999999.000000,
  "depositedAt": "2026-03-30T11:30:00",
  "operator": "attacker",
  "remark": "重复存入尝试"
}
```

**预期响应**：
```json
{
  "code": 400,
  "message": "交易已存在: 0xaabbccddee1122334455667788990011aabbccddee1122334455667788990011",
  "data": null
}
```

**验证要点**：`code`=400，message 包含 "交易已存在"

---

#### ❌ 用例 1-7：txHash 格式错误（缺少 0x 前缀）

**Request Body**：
```json
{
  "txHash": "aabbccddee1122334455667788990011aabbccddee1122334455667788990011",
  "amount": 100000.000000,
  "depositedAt": "2026-03-30T12:00:00",
  "operator": "test_op"
}
```

**预期响应**：`code`=400，message 包含 "交易哈希格式错误"

---

#### ❌ 用例 1-8：txHash 长度不足（少于 64 位十六进制）

**Request Body**：
```json
{
  "txHash": "0xabc123",
  "amount": 100000.000000,
  "depositedAt": "2026-03-30T12:00:00",
  "operator": "test_op"
}
```

**预期响应**：`code`=400，message 包含 "交易哈希格式错误"

---

#### ❌ 用例 1-9：txHash 包含非十六进制字符

**Request Body**：
```json
{
  "txHash": "0xGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG",
  "amount": 100000.000000,
  "depositedAt": "2026-03-30T12:00:00",
  "operator": "test_op"
}
```

**预期响应**：`code`=400，message 包含 "交易哈希格式错误"

---

#### ❌ 用例 1-10：amount 为零

**Request Body**：
```json
{
  "txHash": "0x4444444444444444444444444444444444444444444444444444444444444444",
  "amount": 0,
  "depositedAt": "2026-03-30T12:00:00",
  "operator": "test_op"
}
```

**预期响应**：`code`=400，message 包含 "存入金额必须大于0"

---

#### ❌ 用例 1-11：amount 为负数

**Request Body**：
```json
{
  "txHash": "0x5555555555555555555555555555555555555555555555555555555555555555",
  "amount": -1000.000000,
  "depositedAt": "2026-03-30T12:00:00",
  "operator": "test_op"
}
```

**预期响应**：`code`=400，message 包含 "存入金额必须大于0"

---

#### ❌ 用例 1-12：缺少必填字段 txHash

**Request Body**：
```json
{
  "amount": 100000.000000,
  "depositedAt": "2026-03-30T12:00:00",
  "operator": "test_op"
}
```

**预期响应**：`code`=400，message 包含 "交易哈希不能为空"

---

#### ❌ 用例 1-13：缺少必填字段 operator

**Request Body**：
```json
{
  "txHash": "0x6666666666666666666666666666666666666666666666666666666666666666",
  "amount": 100000.000000,
  "depositedAt": "2026-03-30T12:00:00"
}
```

**预期响应**：`code`=400，message 包含 "操作员不能为空"

---

#### ❌ 用例 1-14：缺少必填字段 depositedAt

**Request Body**：
```json
{
  "txHash": "0x7777777777777777777777777777777777777777777777777777777777777777",
  "amount": 100000.000000,
  "operator": "test_op"
}
```

**预期响应**：`code`=400，message 包含 "存入时间不能为空"

---

#### ❌ 用例 1-15：Request Body 为空 `{}`

**说明**：所有字段均缺失，验证批量校验报错。

**Request Body**：`{}`

**预期响应**：`code`=400，同时出现多个字段的校验错误

---

### 接口 2：GET `/api/collateral/deposits` — 查询存入记录列表

#### ✅ 用例 2-1：无参数查询所有记录

**Swagger 参数**：start 和 end 均**留空**

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {"id": 1, "txHash": "0xaabb...", "amount": 1000000.000000, "status": "CONFIRMED"},
    {"id": 2, "txHash": "0x1111...", "amount": 50000000.000000, "status": "CONFIRMED"}
  ]
}
```

**验证要点**：返回数组，包含之前所有存入记录

---

#### ✅ 用例 2-2：指定今日时间范围查询

**Swagger 参数**：
- `start` = `2026-03-30T00:00:00`
- `end` = `2026-03-30T23:59:59`

**预期响应**：`code`=200，返回今日存入记录

---

#### ✅ 用例 2-3：时间范围内无数据（历史空窗口）

**Swagger 参数**：
- `start` = `2020-01-01T00:00:00`
- `end` = `2020-01-01T23:59:59`

**预期响应**：
```json
{"code": 200, "message": "success", "data": []}
```

**验证要点**：返回空数组，不报错，不返回 null

---

#### ✅ 用例 2-4：只传 start 不传 end

**Swagger 参数**：
- `start` = `2026-03-30T00:00:00`
- `end` 留空

**预期响应**：`code`=200，返回 start 之后的所有记录（不报 400）

---

#### ✅ 用例 2-5：只传 end 不传 start

**Swagger 参数**：
- `start` 留空
- `end` = `2026-03-30T23:59:59`

**预期响应**：`code`=200，返回 end 之前的所有记录（不报 400）

---

#### ✅ 用例 2-6：start 与 end 相同（精确时间点）

**Swagger 参数**：
- `start` = `2026-03-30T09:00:00`
- `end` = `2026-03-30T09:00:00`

**预期响应**：`code`=200，返回该精确时间存入的记录（可能为空数组）

---

#### ❌ 用例 2-7：start 晚于 end（时间倒置）

**Swagger 参数**：
- `start` = `2026-03-30T23:59:59`
- `end` = `2026-03-30T00:00:00`

**预期响应**：`code`=200 返回空数组，或 `code`=400 提示时间参数错误（均可接受，不应 500）

---

### 接口 3：GET `/api/collateral/deposits/{txHash}` — 根据交易哈希查询

#### ✅ 用例 3-1：查询已存在的交易

**Path 参数**：
```
txHash = 0xaabbccddee1122334455667788990011aabbccddee1122334455667788990011
```

**预期响应**：
```json
{
  "code": 200,
  "data": {
    "txHash": "0xaabbccddee1122334455667788990011aabbccddee1122334455667788990011",
    "amount": 1000000.000000,
    "status": "CONFIRMED",
    "operator": "treasury_dept_001",
    "currency": "USD"
  }
}
```

---

#### ✅ 用例 3-2：查询大额存入记录

**Path 参数**：
```
txHash = 0x1111111111111111111111111111111111111111111111111111111111111111
```

**预期响应**：`code`=200，`amount`=50000000.000000

---

#### ❌ 用例 3-3：查询不存在的交易哈希

**Path 参数**：
```
txHash = 0x0000000000000000000000000000000000000000000000000000000000000000
```

**预期响应**：`code`=404 或 400，message 包含 "不存在"，不返回 500

---

#### ❌ 用例 3-4：txHash 格式不合法（路径参数）

**Path 参数**：
```
txHash = invalid_hash
```

**预期响应**：`code`=400 或 404，不发生 500 错误

---

### 接口 4：POST `/api/collateral/reserve-check` — 储备验证（简化版）

#### ✅ 用例 4-1：正常流通量验证

**Query 参数**：`stablecoinSupply` = `1000000`

**预期响应**：`code`=200，返回当前总储备金额（BigDecimal）

---

#### ✅ 用例 4-2：极大流通量（储备不足场景）

**Query 参数**：`stablecoinSupply` = `999999999999`

**预期响应**：`code`=200，返回当前总储备（不报错，供调用方判断是否充足）

---

#### ✅ 用例 4-3：流通量为 0

**Query 参数**：`stablecoinSupply` = `0`

**预期响应**：`code`=200，不发生除零异常

---

#### ❌ 用例 4-4：缺少必填参数 stablecoinSupply

**Swagger 参数**：stablecoinSupply 留空不填

**预期响应**：`code`=400，提示参数缺失

---

## 二、储备管理

### 接口 5：GET `/api/reserve/dashboard` — 仪表盘数据

#### ✅ 用例 5-1：获取仪表盘（无参数）

**Swagger 操作**：直接点击 Execute

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalReserve": 51000500.000001,
    "lockedAmount": 0.000000,
    "availableReserve": 51000500.000001,
    "stablecoinSupply": 0.000000,
    "reserveRatio": 999.999999,
    "riskLevel": "HEALTHY",
    "activeAlertCount": 0,
    "updatedAt": "2026-03-30T09:10:00",
    "assetTotalValue": 103500000000.000000,
    "lowRiskRatio": 0.960000,
    "mediumRiskRatio": 0.030000,
    "highRiskRatio": 0.020000,
    "estimatedAnnualIncome": 5390000000.000000
  }
}
```

**验证要点**：
- `riskLevel` 为 `HEALTHY` / `NORMAL` / `WARNING` / `CRITICAL` 之一
- 流通量为 0 时，`reserveRatio` 显示 999.999999
- `lowRiskRatio + mediumRiskRatio + highRiskRatio` ≈ 1.0
- `availableReserve` = `totalReserve` - `lockedAmount`

---

#### ✅ 用例 5-2：存入后验证仪表盘数据更新

**前置条件**：已执行用例 1-1（存入 100 万）后调用本接口

**验证要点**：
- `totalReserve` 比初始值增加约 1000000
- `availableReserve` 同步增加
- `activeAlertCount` 仍为 0（储备充足）

---

### 接口 6：POST `/api/reserve/snapshot` — 手动触发储备快照

#### ✅ 用例 6-1：正常触发快照

**Swagger 操作**：直接点击 Execute（无参数）

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "reserveAmount": 51000500.000001,
    "stablecoinSupply": 0.000000,
    "reserveRatio": 999.999999,
    "riskLevel": "HEALTHY",
    "snapshotAt": "2026-03-30T09:15:00"
  }
}
```

**验证要点**：
- 返回快照记录，`id` 自增
- `snapshotAt` 为当前时间
- `riskLevel` 与仪表盘一致

---

#### ✅ 用例 6-2：多次连续触发快照

**说明**：连续点击 Execute 3 次，验证每次都能正常生成新快照记录。

**验证要点**：
- 每次返回 `code`=200
- 每次返回的 `id` 不同（自增）
- `snapshotAt` 各不相同

---

### 接口 7：GET `/api/reserve/history` — 储备率历史记录

#### ✅ 用例 7-1：查询今日历史（有数据）

**说明**：先执行用例 6-1 生成快照，再查询今日历史。

**Swagger 参数**：
- `start` = `2026-03-30T00:00:00`
- `end` = `2026-03-30T23:59:59`

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "reserveAmount": 51000500.000001,
      "stablecoinSupply": 0.000000,
      "reserveRatio": 999.999999,
      "riskLevel": "HEALTHY",
      "snapshotAt": "2026-03-30T09:15:00"
    }
  ]
}
```

**验证要点**：返回数组，每条记录包含 `reserveRatio`、`riskLevel`、`snapshotAt`

---

#### ✅ 用例 7-2：跨天时间范围查询

**Swagger 参数**：
- `start` = `2026-03-29T00:00:00`
- `end` = `2026-03-30T23:59:59`

**预期响应**：`code`=200，返回两天内所有快照，可能含多条

---

#### ✅ 用例 7-3：时间范围内无快照数据

**Swagger 参数**：
- `start` = `2020-01-01T00:00:00`
- `end` = `2020-01-01T23:59:59`

**预期响应**：
```json
{"code": 200, "message": "success", "data": []}
```

**验证要点**：返回空数组，不报错

---

#### ✅ 用例 7-4：时间范围为 1 分钟（精确窗口）

**Swagger 参数**：
- `start` = `2026-03-30T09:14:00`
- `end` = `2026-03-30T09:16:00`

**预期响应**：`code`=200，包含用例 6-1 生成的快照（若时间吻合）

---

#### ❌ 用例 7-5：缺少必填参数 start

**Swagger 参数**：`start` 留空，`end` = `2026-03-30T23:59:59`

**预期响应**：`code`=400，提示 start 参数缺失

---

#### ❌ 用例 7-6：缺少必填参数 end

**Swagger 参数**：`start` = `2026-03-30T00:00:00`，`end` 留空

**预期响应**：`code`=400，提示 end 参数缺失

---

#### ❌ 用例 7-7：start 晚于 end（时间倒置）

**Swagger 参数**：
- `start` = `2026-03-30T23:59:59`
- `end` = `2026-03-30T00:00:00`

**预期响应**：`code`=200 返回空数组，或 `code`=400（均可接受，不应 500）

---

### 接口 8：POST `/api/reserve/check` — 验证储备是否支持发行

#### ✅ 用例 8-1：储备充足（流通量远小于储备）

**说明**：验证储备支持 100 万流通量，当前储备约 5100 万，储备率 >> 1.1

**Swagger 参数**：`stablecoinSupply` = `1000000`

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reserveRatio": 51.000500,
    "riskLevel": "HEALTHY",
    "available": true,
    "totalReserve": 51000500.000001,
    "stablecoinSupply": 1000000.000000,
    "message": "Reserve sufficient"
  }
}
```

**验证要点**：`available`=true，`riskLevel`="HEALTHY"

---

#### ✅ 用例 8-2：储备率略高于 HEALTHY 阈值（1.10）

**说明**：配置 healthy-threshold=1.10，传入恰好低于临界点的流通量。

**Swagger 参数**：`stablecoinSupply` = `46000000`

**验证要点**：`reserveRatio` > 1.10，`riskLevel`="HEALTHY"，`available`=true

---

#### ✅ 用例 8-3：储备率处于 NORMAL 区间（1.00~1.10）

**Swagger 参数**：`stablecoinSupply` = `50000000`

**验证要点**：`reserveRatio` 约为 1.02，`riskLevel`="NORMAL"，`available`=true

---

#### ✅ 用例 8-4：储备率处于 WARNING 区间（0.90~1.00）

**Swagger 参数**：`stablecoinSupply` = `55000000`

**验证要点**：`reserveRatio` < 1.00，`riskLevel`="WARNING"，`available`=false

---

#### ✅ 用例 8-5：储备率低于 CRITICAL 阈值（< 0.90）

**Swagger 参数**：`stablecoinSupply` = `999999999999`

**预期响应**：
```json
{
  "code": 200,
  "data": {
    "reserveRatio": 0.000051,
    "riskLevel": "CRITICAL",
    "available": false,
    "message": "Reserve insufficient"
  }
}
```

**验证要点**：`available`=false，`riskLevel`="CRITICAL"

---

#### ✅ 用例 8-6：流通量为 0（边界情况）

**Swagger 参数**：`stablecoinSupply` = `0`

**预期响应**：`code`=200，`reserveRatio`=999.999999，不发生除零异常

---

#### ✅ 用例 8-7：流通量等于总储备（1:1 精确覆盖）

**说明**：传入与当前总储备完全相同的流通量，储备率恰好为 1.0。

**Swagger 参数**：`stablecoinSupply` = `51000500`（与当前储备接近）

**验证要点**：`reserveRatio` ≈ 1.000000，`riskLevel`="NORMAL" 或 "WARNING"

---

#### ❌ 用例 8-8：缺少必填参数 stablecoinSupply

**Swagger 参数**：stablecoinSupply 留空不填

**预期响应**：`code`=400，提示参数缺失

---

#### ❌ 用例 8-9：stablecoinSupply 为负数

**Swagger 参数**：`stablecoinSupply` = `-1`

**预期响应**：`code`=400，或 `code`=200 但结果不合逻辑（需记录）

---

## 三、储备资产

### 接口 9：GET `/api/assets/portfolio` — 获取完整资产组合

#### ✅ 用例 9-1：获取全部资产组合（无参数）

**Swagger 操作**：直接点击 Execute

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "assetType": "US_TREASURY",
      "assetName": "美国短期国债",
      "usdValue": 86940000000.000000,
      "targetAllocationRatio": 0.840000,
      "actualAllocationRatio": 0.840000,
      "riskLevel": 1,
      "riskLevelName": "LOW",
      "annualYield": 0.053000,
      "estimatedAnnualIncome": 4607820000.000000,
      "description": "美国政府短期债券，安全性最高"
    },
    {
      "assetType": "MONEY_MARKET",
      "assetName": "货币市场基金",
      "usdValue": 4140000000.000000,
      "targetAllocationRatio": 0.040000,
      "actualAllocationRatio": 0.040000,
      "riskLevel": 1,
      "riskLevelName": "LOW",
      "annualYield": 0.052000,
      "estimatedAnnualIncome": 215280000.000000,
      "description": "货币市场基金"
    },
    {
      "assetType": "OVERNIGHT_REPO",
      "assetName": "隔夜逆回购协议",
      "riskLevel": 1,
      "riskLevelName": "LOW",
      "annualYield": 0.054000
    },
    {
      "assetType": "CASH",
      "assetName": "现金及银行存款",
      "riskLevel": 1,
      "riskLevelName": "LOW"
    },
    {
      "assetType": "GOLD",
      "assetName": "实物黄金",
      "riskLevel": 2,
      "riskLevelName": "MEDIUM",
      "annualYield": 0.000000
    },
    {
      "assetType": "BITCOIN",
      "assetName": "比特币",
      "riskLevel": 3,
      "riskLevelName": "HIGH",
      "annualYield": 0.000000
    },
    {
      "assetType": "OTHER",
      "assetName": "其他资产",
      "riskLevel": 2,
      "riskLevelName": "MEDIUM"
    }
  ]
}
```

**验证要点**：
- 返回 7 种资产（US_TREASURY / MONEY_MARKET / OVERNIGHT_REPO / CASH / GOLD / BITCOIN / OTHER）
- 每条记录的 `actualAllocationRatio` 为动态计算值（不一定等于 `targetAllocationRatio`）
- 所有 `actualAllocationRatio` 之和约等于 1.0
- `riskLevelName` 与 `riskLevel` 对应：1=LOW，2=MEDIUM，3=HIGH
- `estimatedAnnualIncome` = `usdValue` × `annualYield`

---

#### ✅ 用例 9-2：多次调用结果一致性（幂等验证）

**说明**：连续调用两次，验证返回结果稳定不变。

**验证要点**：两次调用返回完全相同的 `usdValue` 和 `actualAllocationRatio`

---

### 接口 10：GET `/api/assets/assets/{assetType}` — 获取单个资产详情

#### ✅ 用例 10-1：查询 US_TREASURY（美国短期国债）

**Path 参数**：`assetType` = `US_TREASURY`

**预期响应**：
```json
{
  "code": 200,
  "data": {
    "assetType": "US_TREASURY",
    "assetName": "美国短期国债",
    "riskLevel": 1,
    "riskLevelName": "LOW",
    "annualYield": 0.053000,
    "targetAllocationRatio": 0.840000
  }
}
```

---

#### ✅ 用例 10-2：查询 MONEY_MARKET（货币市场基金）

**Path 参数**：`assetType` = `MONEY_MARKET`

**预期响应**：`code`=200，`riskLevel`=1，`annualYield`=0.052000

---

#### ✅ 用例 10-3：查询 OVERNIGHT_REPO（隔夜逆回购）

**Path 参数**：`assetType` = `OVERNIGHT_REPO`

**预期响应**：`code`=200，`riskLevel`=1，`annualYield`=0.054000

---

#### ✅ 用例 10-4：查询 CASH（现金）

**Path 参数**：`assetType` = `CASH`

**预期响应**：`code`=200，`riskLevel`=1，`annualYield`=0.001000

---

#### ✅ 用例 10-5：查询 GOLD（黄金）

**Path 参数**：`assetType` = `GOLD`

**预期响应**：`code`=200，`riskLevel`=2，`riskLevelName`="MEDIUM"，`annualYield`=0.000000

---

#### ✅ 用例 10-6：查询 BITCOIN（比特币）

**Path 参数**：`assetType` = `BITCOIN`

**预期响应**：`code`=200，`riskLevel`=3，`riskLevelName`="HIGH"，`annualYield`=0.000000

---

#### ✅ 用例 10-7：查询 OTHER（其他资产）

**Path 参数**：`assetType` = `OTHER`

**预期响应**：`code`=200，`riskLevel`=2，`annualYield`=0.030000

---

#### ❌ 用例 10-8：查询不存在的资产类型

**Path 参数**：`assetType` = `ETHEREUM`

**预期响应**：`code`=404 或 400，message 包含 "不存在" 或 "找不到资产"，不返回 500

---

#### ❌ 用例 10-9：资产类型为小写（大小写敏感测试）

**Path 参数**：`assetType` = `us_treasury`

**预期响应**：`code`=404 或 400（系统使用大写枚举，小写应无法匹配），不返回 500

---

#### ❌ 用例 10-10：资产类型为空字符串

**Path 参数**：`assetType` = ` `（空格）

**预期响应**：`code`=400 或 404，不发生 500

---

## 四、风险警报

### 接口 11：GET `/api/alerts/active` — 查询所有活跃警报

#### ✅ 用例 11-1：系统正常时查询活跃警报（应为空）

**说明**：当储备充足时，活跃警报列表应为空。

**Swagger 操作**：直接点击 Execute

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": []
}
```

**验证要点**：`data` 为空数组，不返回 null

---

#### ✅ 用例 11-2：触发警报后再查询（有数据场景）

**前置条件**：先调用 `POST /api/reserve/check?stablecoinSupply=999999999999` 触发 CRITICAL 级别警报，再调用本接口。

**预期响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "alertType": "RESERVE_INSUFFICIENT",
      "riskLevel": "CRITICAL",
      "reserveRatio": 0.000051,
      "gapAmount": 948999448999.000000,
      "description": "储备严重不足",
      "status": "ACTIVE",
      "createdAt": "2026-03-30T09:30:00"
    }
  ]
}
```

**验证要点**：
- `status` = "ACTIVE"
- `alertType` 为 `RESERVE_INSUFFICIENT` / `LARGE_REDEMPTION` / `DATA_INCONSISTENCY` 之一
- `riskLevel` 为 `WARNING` / `CRITICAL` 之一
- `reserveRatio` 与 check 接口返回一致

---

#### ✅ 用例 11-3：警报确认后再查询活跃警报

**前置条件**：已执行用例 11-2（存在活跃警报），再执行用例 12-1 确认警报

**验证要点**：已 ACKNOWLEDGED 的警报仍出现在活跃列表（status=ACKNOWLEDGED），还是被过滤掉（视业务定义）

---

### 接口 12：PUT `/api/alerts/{id}/acknowledge` — 确认警报

#### ✅ 用例 12-1：正常确认警报

**前置条件**：系统中存在 id=1 的 ACTIVE 警报（先执行用例 11-2）

**Path 参数**：`id` = `1`

**Query 参数**：`operator` = `admin_user`

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "status": "ACKNOWLEDGED",
    "resolvedBy": "admin_user",
    "updatedAt": "2026-03-30T09:35:00"
  }
}
```

**验证要点**：`status` 变为 "ACKNOWLEDGED"，`resolvedBy` = "admin_user"

---

#### ✅ 用例 12-2：不同操作员确认同一警报

**说明**：已 ACKNOWLEDGED 的警报再次被另一操作员确认，验证系统行为。

**Path 参数**：`id` = `1`

**Query 参数**：`operator` = `supervisor_001`

**预期响应**：`code`=200（覆盖更新）或 `code`=400（状态不允许重复确认）

---

#### ❌ 用例 12-3：确认不存在的警报 ID

**Path 参数**：`id` = `99999`

**Query 参数**：`operator` = `admin_user`

**预期响应**：`code`=404 或 400，message 包含 "不存在"，不返回 500

---

#### ❌ 用例 12-4：operator 为空字符串

**Path 参数**：`id` = `1`

**Query 参数**：`operator` = ` `（空格）

**预期响应**：`code`=400，提示操作员不能为空，或 `code`=200（视校验是否存在）

---

#### ❌ 用例 12-5：缺少必填参数 operator

**Path 参数**：`id` = `1`

**Swagger 参数**：operator 留空不填

**预期响应**：`code`=400，提示参数缺失

---

### 接口 13：PUT `/api/alerts/{id}/resolve` — 解决警报

#### ✅ 用例 13-1：正常解决 ACTIVE 警报

**前置条件**：系统中存在 id=1 的 ACTIVE 警报

**Path 参数**：`id` = `1`

**Query 参数**：`operator` = `senior_admin`

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "status": "RESOLVED",
    "resolvedBy": "senior_admin",
    "resolvedAt": "2026-03-30T09:40:00"
  }
}
```

**验证要点**：`status` = "RESOLVED"，`resolvedAt` 有值，`resolvedBy` = "senior_admin"

---

#### ✅ 用例 13-2：解决 ACKNOWLEDGED 状态的警报

**前置条件**：先执行用例 12-1（将警报置为 ACKNOWLEDGED），再调用本接口

**Path 参数**：`id` = `1`

**Query 参数**：`operator` = `senior_admin`

**验证要点**：`status` 由 ACKNOWLEDGED 变为 RESOLVED

---

#### ✅ 用例 13-3：解决后验证不再出现在活跃列表

**前置条件**：执行用例 13-1 解决警报后，调用 `GET /api/alerts/active`

**验证要点**：id=1 的警报不再出现在活跃警报列表中（若系统过滤 RESOLVED 状态）

---

#### ❌ 用例 13-4：解决不存在的警报 ID

**Path 参数**：`id` = `88888`

**Query 参数**：`operator` = `admin`

**预期响应**：`code`=404 或 400，不返回 500

---

#### ❌ 用例 13-5：id 为 0（边界值）

**Path 参数**：`id` = `0`

**Query 参数**：`operator` = `admin`

**预期响应**：`code`=404 或 400，不返回 500

---

#### ❌ 用例 13-6：id 为负数

**Path 参数**：`id` = `-1`

**Query 参数**：`operator` = `admin`

**预期响应**：`code`=400 或 404，不返回 500

---

#### ❌ 用例 13-7：缺少必填参数 operator

**Path 参数**：`id` = `1`

**Swagger 参数**：operator 留空不填

**预期响应**：`code`=400，提示参数缺失

---

## 五、审计报告

### 接口 14：GET `/api/audit/reports` — 查询所有审计报告

#### ✅ 用例 14-1：查询所有报告（系统初始状态）

**Swagger 操作**：直接点击 Execute

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": []
}
```

**验证要点**：系统刚启动无手动生成时返回空数组，不报错

---

#### ✅ 用例 14-2：生成报告后再查询（有数据）

**前置条件**：先执行用例 16-1（手动生成报告），再调用本接口

**预期响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "reportNo": "RPT-20260330-093000",
      "periodStart": "2026-03-30T00:00:00",
      "periodEnd": "2026-03-30T09:30:00",
      "avgRatio": 999.999999,
      "minRatio": 999.999999,
      "maxRatio": 999.999999,
      "totalDeposit": 51000500.000001,
      "alertCount": 0,
      "generatedBy": "admin",
      "generatedAt": "2026-03-30T09:30:00"
    }
  ]
}
```

**验证要点**：
- `reportNo` 格式为 `RPT-yyyyMMdd-HHmmss`
- `avgRatio`、`minRatio`、`maxRatio` 均有值
- `generatedAt` 为报告生成时间

---

### 接口 15：GET `/api/audit/reports/{reportNo}` — 根据报告编号查询

#### ✅ 用例 15-1：查询已存在的报告

**前置条件**：先执行用例 16-1 生成报告，获取 reportNo

**Path 参数**（替换为实际生成的报告编号）：
```
reportNo = RPT-20260330-093000
```

**预期响应**：
```json
{
  "code": 200,
  "data": {
    "reportNo": "RPT-20260330-093000",
    "periodStart": "2026-03-30T00:00:00",
    "periodEnd": "2026-03-30T09:30:00",
    "avgRatio": 999.999999,
    "minRatio": 999.999999,
    "maxRatio": 999.999999,
    "totalDeposit": 51000500.000001,
    "alertCount": 0,
    "reportDataJson": "{...}",
    "generatedBy": "admin",
    "generatedAt": "2026-03-30T09:30:00"
  }
}
```

**验证要点**：包含完整的 `reportDataJson` 字段

---

#### ❌ 用例 15-2：查询不存在的报告编号

**Path 参数**：`reportNo` = `RPT-19990101-000000`

**预期响应**：`code`=404 或 400，message 包含 "不存在"，不返回 500

---

#### ❌ 用例 15-3：报告编号格式错误

**Path 参数**：`reportNo` = `INVALID_REPORT_NO`

**预期响应**：`code`=404 或 400，不发生 500

---

#### ❌ 用例 15-4：报告编号包含特殊字符

**Path 参数**：`reportNo` = `RPT-2026/03/30`

**预期响应**：`code`=400 或 404，不返回 500

---

### 接口 16：POST `/api/audit/reports/generate` — 手动生成审计报告

#### ✅ 用例 16-1：正常生成今日报告

**说明**：手动生成今日 0 点到当前时间的报告。

**Swagger 参数**：
- `start` = `2026-03-30T00:00:00`
- `end` = `2026-03-30T23:59:59`
- `operator` = `admin`

**预期响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "reportNo": "RPT-20260330-XXXXXX",
    "periodStart": "2026-03-30T00:00:00",
    "periodEnd": "2026-03-30T23:59:59",
    "avgRatio": 999.999999,
    "totalDeposit": 51000500.000001,
    "alertCount": 0,
    "generatedBy": "admin",
    "generatedAt": "2026-03-30T..."
  }
}
```

**验证要点**：
- `reportNo` 格式正确：`RPT-` + 日期 + `-` + 时间
- `generatedBy` = "admin"
- `periodStart`/`periodEnd` 与请求参数一致

---

#### ✅ 用例 16-2：生成跨天报告（昨天全天）

**Swagger 参数**：
- `start` = `2026-03-29T00:00:00`
- `end` = `2026-03-29T23:59:59`
- `operator` = `scheduler_test`

**预期响应**：`code`=200，`periodStart`/`periodEnd` 与参数一致

---

#### ✅ 用例 16-3：使用默认 operator（不传 operator 参数）

**Swagger 参数**：
- `start` = `2026-03-30T08:00:00`
- `end` = `2026-03-30T09:00:00`
- `operator` 留空（使用默认值 "manual-trigger"）

**预期响应**：`code`=200，`generatedBy` = "manual-trigger"

---

#### ✅ 用例 16-4：时间范围内无快照数据

**说明**：指定一个历史空窗口，验证无快照数据时仍能生成报告（统计值为 null 或 0）。

**Swagger 参数**：
- `start` = `2020-01-01T00:00:00`
- `end` = `2020-01-01T23:59:59`
- `operator` = `test_user`

**预期响应**：
- `code`=200，`avgRatio`=null 或 0，`totalDeposit`=0，`alertCount`=0
- 不发生异常

---

#### ✅ 用例 16-5：时间范围仅 1 小时

**Swagger 参数**：
- `start` = `2026-03-30T09:00:00`
- `end` = `2026-03-30T10:00:00`
- `operator` = `hourly_admin`

**预期响应**：`code`=200，报告正常生成

---

#### ❌ 用例 16-6：缺少必填参数 start

**Swagger 参数**：`start` 留空，`end` = `2026-03-30T23:59:59`，`operator` = `admin`

**预期响应**：`code`=400，提示 start 参数缺失

---

#### ❌ 用例 16-7：缺少必填参数 end

**Swagger 参数**：`start` = `2026-03-30T00:00:00`，`end` 留空，`operator` = `admin`

**预期响应**：`code`=400，提示 end 参数缺失

---

#### ❌ 用例 16-8：start 晚于 end（时间倒置）

**Swagger 参数**：
- `start` = `2026-03-30T23:59:59`
- `end` = `2026-03-30T00:00:00`
- `operator` = `admin`

**预期响应**：`code`=400（时间参数错误）或 `code`=200（返回空报告），不应 500

---

#### ❌ 用例 16-9：重复生成同一时间段的报告

**说明**：对同一时间段连续调用两次，验证是否允许重复生成。

**Swagger 参数**：
- `start` = `2026-03-30T00:00:00`
- `end` = `2026-03-30T23:59:59`
- `operator` = `admin`

**操作**：连续点击 Execute 两次

**预期响应**：两次均返回 `code`=200，但 `reportNo` 不同（时间戳不同），或第二次返回 400（不允许重复）

---

## 六、推荐执行顺序

以下是在 Swagger 中推荐的完整测试执行流程，确保数据依赖关系正确：

```
第一阶段：基础数据准备
──────────────────────────────────────────────────────
步骤 1  GET  /api/reserve/dashboard          → 记录初始 totalReserve（基线值）
步骤 2  GET  /api/assets/portfolio           → 确认 7 种资产均已初始化
步骤 3  GET  /api/assets/assets/US_TREASURY  → 确认单个资产查询正常
步骤 4  GET  /api/alerts/active              → 确认初始无活跃警报
步骤 5  GET  /api/audit/reports             → 确认初始无审计报告

第二阶段：抵押物存入
──────────────────────────────────────────────────────
步骤 6  POST /api/collateral/deposit         → 用例 1-1（存入 100 万）
步骤 7  POST /api/collateral/deposit         → 用例 1-2（存入 5000 万）
步骤 8  POST /api/collateral/deposit         → 用例 1-3（存入 0.000001）
步骤 9  GET  /api/collateral/deposits        → 用例 2-1（查询所有，应有 3 条）
步骤 10 GET  /api/collateral/deposits/{txHash} → 用例 3-1（按哈希查询第1笔）
步骤 11 POST /api/collateral/reserve-check    → 用例 4-1（简化储备验证）

第三阶段：储备验证与快照
──────────────────────────────────────────────────────
步骤 12 GET  /api/reserve/dashboard          → 用例 5-2（验证 totalReserve 已增加）
步骤 13 POST /api/reserve/snapshot           → 用例 6-1（生成快照）
步骤 14 POST /api/reserve/snapshot           → 用例 6-2（再次生成，验证 id 自增）
步骤 15 GET  /api/reserve/history            → 用例 7-1（查询今日历史，应有快照）
步骤 16 POST /api/reserve/check              → 用例 8-1（流通量 100 万，HEALTHY）
步骤 17 POST /api/reserve/check              → 用例 8-3（流通量 5000 万，NORMAL）
步骤 18 POST /api/reserve/check              → 用例 8-4（流通量 5500 万，WARNING）
步骤 19 POST /api/reserve/check              → 用例 8-5（流通量极大，CRITICAL）

第四阶段：警报管理
──────────────────────────────────────────────────────
步骤 20 GET  /api/alerts/active              → 用例 11-2（警报触发后查询，应有记录）
步骤 21 PUT  /api/alerts/1/acknowledge       → 用例 12-1（确认警报，记录返回的 id）
步骤 22 GET  /api/alerts/active              → 用例 11-3（确认后再查询）
步骤 23 PUT  /api/alerts/1/resolve           → 用例 13-1（解决警报）
步骤 24 GET  /api/alerts/active              → 用例 13-3（解决后验证列表清空）

第五阶段：审计报告
──────────────────────────────────────────────────────
步骤 25 POST /api/audit/reports/generate     → 用例 16-1（生成今日报告，记录 reportNo）
步骤 26 GET  /api/audit/reports             → 用例 14-2（查询所有，应有1条）
步骤 27 GET  /api/audit/reports/{reportNo}  → 用例 15-1（用步骤25的 reportNo 查询）
步骤 28 POST /api/audit/reports/generate     → 用例 16-3（不传 operator，验证默认值）

第六阶段：异常场景覆盖
──────────────────────────────────────────────────────
步骤 29 POST /api/collateral/deposit         → 用例 1-6（重复 txHash，应返回 400）
步骤 30 POST /api/collateral/deposit         → 用例 1-7（格式错误 txHash，应返回 400）
步骤 31 POST /api/collateral/deposit         → 用例 1-10（amount=0，应返回 400）
步骤 32 POST /api/collateral/deposit         → 用例 1-12（缺少 txHash，应返回 400）
步骤 33 GET  /api/collateral/deposits/{txHash} → 用例 3-3（不存在的哈希，应返回 404）
步骤 34 GET  /api/assets/assets/ETHEREUM    → 用例 10-8（不存在资产类型，应返回 404）
步骤 35 GET  /api/reserve/history           → 用例 7-5（缺少 start，应返回 400）
步骤 36 GET  /api/audit/reports/RPT-19990101-000000 → 用例 15-2（不存在报告，应返回 404）
步骤 37 PUT  /api/alerts/99999/acknowledge  → 用例 12-3（不存在 id，应返回 404）
步骤 38 PUT  /api/alerts/99999/resolve      → 用例 13-4（不存在 id，应返回 404）
```

---

## 附录：各接口参数速查表

| 接口 | 方法 | 路径 | 参数类型 | 必填参数 |
|------|------|------|----------|----------|
| 抵押物存入 | POST | `/api/collateral/deposit` | Body(JSON) | txHash, amount, depositedAt, operator |
| 查询存入列表 | GET | `/api/collateral/deposits` | Query | 无（start/end 可选） |
| 按哈希查询 | GET | `/api/collateral/deposits/{txHash}` | Path | txHash |
| 储备验证（简） | POST | `/api/collateral/reserve-check` | Query | stablecoinSupply |
| 仪表盘 | GET | `/api/reserve/dashboard` | 无 | 无 |
| 手动快照 | POST | `/api/reserve/snapshot` | 无 | 无 |
| 历史记录 | GET | `/api/reserve/history` | Query | start, end（**必填**） |
| 储备验证 | POST | `/api/reserve/check` | Query | stablecoinSupply |
| 资产组合 | GET | `/api/assets/portfolio` | 无 | 无 |
| 单个资产 | GET | `/api/assets/assets/{assetType}` | Path | assetType |
| 活跃警报 | GET | `/api/alerts/active` | 无 | 无 |
| 确认警报 | PUT | `/api/alerts/{id}/acknowledge` | Path + Query | id, operator |
| 解决警报 | PUT | `/api/alerts/{id}/resolve` | Path + Query | id, operator |
| 所有报告 | GET | `/api/audit/reports` | 无 | 无 |
| 按编号查报告 | GET | `/api/audit/reports/{reportNo}` | Path | reportNo |
| 生成报告 | POST | `/api/audit/reports/generate` | Query | start, end（operator 可选） |

---

## 附录：txHash 速查表

以下哈希值可直接复制到 Swagger 使用：

```
✅ 有效格式（可用于正常存入）：
0xaabbccddee1122334455667788990011aabbccddee1122334455667788990011  （用例 1-1，100万）
0x1111111111111111111111111111111111111111111111111111111111111111  （用例 1-2，5000万）
0x2222222222222222222222222222222222222222222222222222222222222222  （用例 1-3，最小额）
0x3333333333333333333333333333333333333333333333333333333333333333  （用例 1-4，无remark）
0x9999999999999999999999999999999999999999999999999999999999999999  （用例 1-5，未来时间）
0x4444444444444444444444444444444444444444444444444444444444444444  （备用）
0x5555555555555555555555555555555555555555555555555555555555555555  （备用）
0x6666666666666666666666666666666666666666666666666666666666666666  （备用）
0x7777777777777777777777777777777777777777777777777777777777777777  （备用）
0x8888888888888888888888888888888888888888888888888888888888888888  （备用）

❌ 无效格式（用于异常测试）：
aabbccddee1122334455667788990011aabbccddee1122334455667788990011  （缺少0x前缀）
0xabc123                                                          （长度不足）
0xGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG  （非十六进制字符）
0x0000000000000000000000000000000000000000000000000000000000000000  （不存在但格式合法，用于查询测试）
```

---

## 附录：资产类型枚举值

```
US_TREASURY    — 美国短期国债   riskLevel=1(LOW)    annualYield=5.3%
MONEY_MARKET   — 货币市场基金   riskLevel=1(LOW)    annualYield=5.2%
OVERNIGHT_REPO — 隔夜逆回购    riskLevel=1(LOW)    annualYield=5.4%
CASH           — 现金及银行存款 riskLevel=1(LOW)    annualYield=0.1%
GOLD           — 实物黄金       riskLevel=2(MEDIUM) annualYield=0%
BITCOIN        — 比特币         riskLevel=3(HIGH)   annualYield=0%
OTHER          — 其他资产       riskLevel=2(MEDIUM) annualYield=3.0%
```

---

## 附录：风险阈值配置说明

根据 `application.yml` 配置：

| 阈值名称 | 配置值 | 说明 |
|----------|--------|------|
| healthy-threshold | 1.10 | 储备率 ≥ 1.10 → HEALTHY |
| normal-threshold | 1.00 | 储备率 1.00~1.10 → NORMAL |
| warning-threshold | 0.90 | 储备率 0.90~1.00 → WARNING |
| 低于 warning | < 0.90 | 储备率 < 0.90 → CRITICAL |

**available 字段规则**：
- HEALTHY → `available: true`
- NORMAL → `available: true`
- WARNING → `available: false`
- CRITICAL → `available: false`

---

*文档版本：1.0 | 生成日期：2026-03-30 | 覆盖接口：16个，测试用例：80+*
