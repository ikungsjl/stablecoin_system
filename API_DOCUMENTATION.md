# 稳定币抵押物管理系统（课题2）- API 接口文档

## 目录

1. [抵押物管理](#抵押物管理)
2. [储备验证](#储备验证)
3. [审计报告](#审计报告)
4. [储备资产](#储备资产)
5. [风险告警](#风险告警)

---

## 抵押物管理

### 1. 存入抵押物

**接口描述**：前端课题传入交易哈希和时间戳，课题2验证并记录存入

**请求方法**：`POST`

**请求路径**：`/api/collateral/deposit`

**请求头**：
```
Content-Type: application/json
```

**请求体**：
```json
{
  "txHash": "0x3f2a7c9e1b4d8a2c5e9f1a3b6c8d0e2f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c",
  "amount": 1000000.000000,
  "depositedAt": "2026-03-28T15:04:02",
  "operator": "treasury_dept_001",
  "remark": "Initial collateral deposit"
}
```

**请求参数说明**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| txHash | String | 是 | 交易哈希，格式：0x + 64位十六进制，唯一标识 |
| amount | BigDecimal | 是 | 存入金额（USD），必须 > 0 |
| depositedAt | LocalDateTime | 是 | 存入时间，格式：yyyy-MM-ddTHH:mm:ss |
| operator | String | 是 | 操作员标识，长度 1-100 |
| remark | String | 否 | 备注信息 |

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "txHash": "0x3f2a7c9e1b4d8a2c5e9f1a3b6c8d0e2f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c",
    "amount": 1000000.000000,
    "currency": "USD",
    "usdAmount": 1000000.000000,
    "exchangeRate": 1.000000,
    "operator": "treasury_dept_001",
    "status": "CONFIRMED",
    "remark": "Initial collateral deposit",
    "depositedAt": "2026-03-28T15:04:02",
    "createdAt": "2026-03-28T15:04:02.123456",
    "updatedAt": "2026-03-28T15:04:02.123456"
  }
}
```

**响应参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 记录ID |
| txHash | String | 交易哈希 |
| amount | BigDecimal | 存入金额 |
| currency | String | 货币类型（固定为 USD） |
| usdAmount | BigDecimal | USD 金额 |
| exchangeRate | BigDecimal | 汇率（固定为 1.0） |
| operator | String | 操作员 |
| status | String | 状态（CONFIRMED/PENDING/FAILED） |
| remark | String | 备注 |
| depositedAt | LocalDateTime | 存入时间 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

**错误响应**：
```json
{
  "code": 400,
  "message": "交易已存在: 0x3f2a...",
  "data": null
}
```

**HTTP 状态码**：
- 200: 成功
- 400: 参数错误或交易重复
- 500: 服务器错误

---

### 2. 查询存入记录

**接口描述**：查询所有存入记录或指定时间范围内的记录

**请求方法**：`GET`

**请求路径**：`/api/collateral/deposits`

**查询参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| start | LocalDateTime | 否 | 开始时间，格式：yyyy-MM-ddTHH:mm:ss |
| end | LocalDateTime | 否 | 结束时间，格式：yyyy-MM-ddTHH:mm:ss |

**请求示例**：
```
GET /api/collateral/deposits?start=2026-03-28T00:00:00&end=2026-03-28T23:59:59
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "txHash": "0x3f2a...",
      "amount": 1000000.000000,
      "currency": "USD",
      "usdAmount": 1000000.000000,
      "exchangeRate": 1.000000,
      "operator": "treasury_dept_001",
      "status": "CONFIRMED",
      "remark": "Initial collateral deposit",
      "depositedAt": "2026-03-28T15:04:02",
      "createdAt": "2026-03-28T15:04:02.123456",
      "updatedAt": "2026-03-28T15:04:02.123456"
    }
  ]
}
```

---

### 3. 根据交易哈希查询

**接口描述**：根据交易哈希查询单笔存入记录

**请求方法**：`GET`

**请求路径**：`/api/collateral/deposits/{txHash}`

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| txHash | String | 交易哈希 |

**请求示例**：
```
GET /api/collateral/deposits/0x3f2a7c9e1b4d8a2c5e9f1a3b6c8d0e2f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "txHash": "0x3f2a...",
    "amount": 1000000.000000,
    ...
  }
}
```

---

## 储备验证

### 4. 查询仪表盘

**接口描述**：获取实时的储备状态、风险等级、资产配置等信息

**请求方法**：`GET`

**请求路径**：`/api/reserve/dashboard`

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalReserve": 10000000.000000,
    "lockedAmount": 0.000000,
    "availableReserve": 10000000.000000,
    "stablecoinSupply": 0.000000,
    "reserveRatio": 999.999999,
    "riskLevel": "HEALTHY",
    "activeAlertCount": 0,
    "updatedAt": "2026-03-28T15:04:02",
    "assetTotalValue": 10000000.000000,
    "lowRiskRatio": 0.950000,
    "mediumRiskRatio": 0.030000,
    "highRiskRatio": 0.020000,
    "estimatedAnnualIncome": 530000.000000
  }
}
```

**响应参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| totalReserve | BigDecimal | 总储备金额（USD） |
| lockedAmount | BigDecimal | 锁定金额（用于赎回） |
| availableReserve | BigDecimal | 可用储备 = 总储备 - 锁定 |
| stablecoinSupply | BigDecimal | 稳定币流通量 |
| reserveRatio | BigDecimal | 储备率 = 总储备 / 流通量 |
| riskLevel | String | 风险等级（HEALTHY/NORMAL/WARNING/CRITICAL） |
| activeAlertCount | Long | 活跃告警数 |
| updatedAt | LocalDateTime | 最后更新时间 |
| assetTotalValue | BigDecimal | 资产总价值 |
| lowRiskRatio | BigDecimal | 低风险资产占比 |
| mediumRiskRatio | BigDecimal | 中风险资产占比 |
| highRiskRatio | BigDecimal | 高风险资产占比 |
| estimatedAnnualIncome | BigDecimal | 预期年化收益 |

---

### 5. 手动触发快照

**接口描述**：手动触发一次储备验证快照（通常由定时任务自动执行）

**请求方法**：`POST`

**请求路径**：`/api/reserve/snapshot`

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "reserveAmount": 10000000.000000,
    "stablecoinSupply": 0.000000,
    "reserveRatio": 999.999999,
    "riskLevel": "HEALTHY",
    "snapshotAt": "2026-03-28T15:04:02"
  }
}
```

---

### 6. 查询储备率历史

**接口描述**：查询指定时间范围内的储备率历史（用于绘制趋势图）

**请求方法**：`GET`

**请求路径**：`/api/reserve/history`

**查询参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| start | LocalDateTime | 是 | 开始时间 |
| end | LocalDateTime | 是 | 结束时间 |

**请求示例**：
```
GET /api/reserve/history?start=2026-03-28T00:00:00&end=2026-03-28T23:59:59
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "reserveAmount": 10000000.000000,
      "stablecoinSupply": 0.000000,
      "reserveRatio": 999.999999,
      "riskLevel": "HEALTHY",
      "snapshotAt": "2026-03-28T14:00:00"
    },
    {
      "id": 2,
      "reserveAmount": 10000000.000000,
      "stablecoinSupply": 5000000.000000,
      "reserveRatio": 2.000000,
      "riskLevel": "HEALTHY",
      "snapshotAt": "2026-03-28T15:00:00"
    }
  ]
}
```

---

### 7. 验证储备是否支持发行

**接口描述**：供课题1调用，检查当前储备是否支持继续发行稳定币

**请求方法**：`POST`

**请求路径**：`/api/reserve/check`

**查询参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| stablecoinSupply | BigDecimal | 是 | 假设的稳定币流通量 |

**请求示例**：
```
POST /api/reserve/check?stablecoinSupply=5000000
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "reserveRatio": 2.000000,
    "riskLevel": "HEALTHY",
    "available": true,
    "totalReserve": 10000000.000000,
    "stablecoinSupply": 5000000.000000,
    "message": "Reserve sufficient"
  }
}
```

**响应参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| reserveRatio | BigDecimal | 计算后的储备率 |
| riskLevel | String | 风险等级 |
| available | Boolean | 是否允许发行（true/false） |
| totalReserve | BigDecimal | 当前总储备 |
| stablecoinSupply | BigDecimal | 假设的流通量 |
| message | String | 提示信息 |

---

## 审计报告

### 8. 查询所有审计报告

**接口描述**：查询所有已生成的审计报告

**请求方法**：`GET`

**请求路径**：`/api/audit/reports`

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "reportNo": "RPT-20260328-000000",
      "periodStart": "2026-03-27T00:00:00",
      "periodEnd": "2026-03-27T23:59:59",
      "avgRatio": 1.250000,
      "minRatio": 1.100000,
      "maxRatio": 1.500000,
      "totalDeposit": 3000000.000000,
      "totalIssuance": 0.000000,
      "totalRedemption": 0.000000,
      "alertCount": 0,
      "generatedBy": "system-scheduler",
      "generatedAt": "2026-03-28T00:00:01"
    }
  ]
}
```

---

### 9. 根据报告编号查询

**接口描述**：根据报告编号查询单份审计报告详情

**请求方法**：`GET`

**请求路径**：`/api/audit/reports/{reportNo}`

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| reportNo | String | 报告编号，格式：RPT-yyyyMMdd-HHmmss |

**请求示例**：
```
GET /api/audit/reports/RPT-20260328-000000
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "reportNo": "RPT-20260328-000000",
    "periodStart": "2026-03-27T00:00:00",
    "periodEnd": "2026-03-27T23:59:59",
    "avgRatio": 1.250000,
    "minRatio": 1.100000,
    "maxRatio": 1.500000,
    "totalDeposit": 3000000.000000,
    "alertCount": 0,
    "reportDataJson": "{...详细数据...}",
    "generatedBy": "system-scheduler",
    "generatedAt": "2026-03-28T00:00:01"
  }
}
```

---

### 10. 手动生成审计报告

**接口描述**：手动生成指定时间范围的审计报告

**请求方法**：`POST`

**请求路径**：`/api/audit/reports/generate`

**查询参数**：

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| start | LocalDateTime | 是 | 报告开始时间 |
| end | LocalDateTime | 是 | 报告结束时间 |
| operator | String | 否 | 操作员标识，默认：manual-trigger |

**请求示例**：
```
POST /api/audit/reports/generate?start=2026-03-28T00:00:00&end=2026-03-28T23:59:59&operator=admin
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 2,
    "reportNo": "RPT-20260328-150000",
    "periodStart": "2026-03-28T00:00:00",
    "periodEnd": "2026-03-28T23:59:59",
    "avgRatio": 1.300000,
    "minRatio": 1.200000,
    "maxRatio": 1.400000,
    "totalDeposit": 2000000.000000,
    "alertCount": 0,
    "generatedBy": "admin",
    "generatedAt": "2026-03-28T15:00:00"
  }
}
```

---

## 储备资产

### 11. 获取资产组合

**接口描述**：获取完整的储备资产组合，包含动态计算的实际占比

**请求方法**：`GET`

**请求路径**：`/api/assets/portfolio`

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "assetType": "US_TREASURY",
      "assetName": "美国短期国债",
      "usdValue": 8400000.000000,
      "targetAllocationRatio": 0.840000,
      "actualAllocationRatio": 0.840000,
      "riskLevel": 1,
      "riskLevelName": "LOW",
      "annualYield": 0.053000,
      "estimatedAnnualIncome": 445200.000000,
      "description": "美国政府短期债券，安全性最高"
    },
    {
      "assetType": "BITCOIN",
      "assetName": "比特币",
      "usdValue": 200000.000000,
      "targetAllocationRatio": 0.020000,
      "actualAllocationRatio": 0.020000,
      "riskLevel": 3,
      "riskLevelName": "HIGH",
      "annualYield": 0.000000,
      "estimatedAnnualIncome": 0.000000,
      "description": "比特币资产"
    }
  ]
}
```

**响应参数说明**：

| 参数 | 类型 | 说明 |
|------|------|------|
| assetType | String | 资产类型 |
| assetName | String | 资产名称 |
| usdValue | BigDecimal | 持有价值（USD） |
| targetAllocationRatio | BigDecimal | 目标占比 |
| actualAllocationRatio | BigDecimal | 实际占比（动态计算） |
| riskLevel | Integer | 风险等级（1=LOW, 2=MEDIUM, 3=HIGH） |
| riskLevelName | String | 风险等级名称 |
| annualYield | BigDecimal | 年化收益率 |
| estimatedAnnualIncome | BigDecimal | 预期年化收益 |
| description | String | 描述 |

---

### 12. 获取单个资产

**接口描述**：根据资产类型获取单个资产详情

**请求方法**：`GET`

**请求路径**：`/api/assets/assets/{assetType}`

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| assetType | String | 资产类型（US_TREASURY/MONEY_MARKET/等） |

**请求示例**：
```
GET /api/assets/assets/US_TREASURY
```

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "assetType": "US_TREASURY",
    "assetName": "美国短期国债",
    "usdValue": 8400000.000000,
    "targetAllocationRatio": 0.840000,
    "actualAllocationRatio": 0.840000,
    "riskLevel": 1,
    "riskLevelName": "LOW",
    "annualYield": 0.053000,
    "estimatedAnnualIncome": 445200.000000,
    "description": "美国政府短期债券，安全性最高"
  }
}
```

---

## 风险告警

### 13. 查询风险告警

**接口描述**：查询所有风险告警记录

**请求方法**：`GET`

**请求路径**：`/api/risk/alerts`

**响应体**：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "alertType": "RESERVE_INSUFFICIENT",
      "riskLevel": "WARNING",
      "reserveRatio": 0.950000,
      "gapAmount": 50000.000000,
      "description": "储备不足，储备率低于正常阈值",
      "status": "ACTIVE",
      "createdAt": "2026-03-28T15:04:02",
      "updatedAt": "2026-03-28T15:04:02"
    }
  ]
}
```

---

## 通用响应格式

所有接口都遵循统一的响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

**响应码说明**：

| 状态码 | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

## 时间格式

所有时间参数和响应都使用 ISO 8601 格式：

```
yyyy-MM-ddTHH:mm:ss
例如：2026-03-28T15:04:02
```

---

## 数值精度

所有金额和比率都使用 BigDecimal，精度为 6 位小数：

```
金额：1000000.000000
比率：0.840000
```

---

## 文档版本

- 版本：1.0
- 最后更新：2026-03-28
- 系统：稳定币抵押物管理系统（课题2）
