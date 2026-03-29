#!/bin/bash

# 稳定币抵押物系统集成测试脚本
# 测试模块：抵押物存入、储备验证、审计报告、风险警报

BASE_URL="http://localhost:8081"
TIMESTAMP=$(date +%s%N | cut -b1-13)

echo "=========================================="
echo "稳定币抵押物系统集成测试"
echo "=========================================="
echo ""

# 生成交易哈希（模拟前端生成）
generate_tx_hash() {
    echo "0x$(openssl rand -hex 32)"
}

# 测试 1: 查询初始储备状态
echo "[测试 1] 查询初始储备状态"
echo "GET /api/reserve/dashboard"
curl -s -X GET "$BASE_URL/api/reserve/dashboard" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# 测试 2: 查询资产配置
echo "[测试 2] 查询储备资产配置"
echo "GET /api/assets/portfolio"
curl -s -X GET "$BASE_URL/api/assets/portfolio" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# 测试 3: 存入抵押物（第一笔）
echo "[测试 3] 存入抵押物 - 第一笔 (1000000 USD)"
TX_HASH_1=$(generate_tx_hash)
echo "POST /api/collateral/deposit"
echo "txHash: $TX_HASH_1"
DEPOSIT_1=$(curl -s -X POST "$BASE_URL/api/collateral/deposit" \
  -H "Content-Type: application/json" \
  -d "{
    \"txHash\": \"$TX_HASH_1\",
    \"amount\": 1000000.000000,
    \"depositedAt\": \"$(date -u +'%Y-%m-%dT%H:%M:%S')\",
    \"operator\": \"treasury_dept_001\",
    \"remark\": \"Initial collateral deposit\"
  }")
echo "$DEPOSIT_1" | jq '.'
echo ""
echo ""

# 测试 4: 查询存入记录
echo "[测试 4] 查询存入记录"
echo "GET /api/collateral/deposits"
curl -s -X GET "$BASE_URL/api/collateral/deposits" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# 测试 5: 手动触发储备验证快照
echo "[测试 5] 手动触发储备验证快照"
echo "POST /api/reserve/snapshot"
curl -s -X POST "$BASE_URL/api/reserve/snapshot" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# 测试 6: 查询更新后的仪表盘
echo "[测试 6] 查询更新后的仪表盘（存入后）"
echo "GET /api/reserve/dashboard"
curl -s -X GET "$BASE_URL/api/reserve/dashboard" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# 测试 7: 存入第二笔抵押物
echo "[测试 7] 存入抵押物 - 第二笔 (2000000 USD)"
TX_HASH_2=$(generate_tx_hash)
echo "POST /api/collateral/deposit"
echo "txHash: $TX_HASH_2"
curl -s -X POST "$BASE_URL/api/collateral/deposit" \
  -H "Content-Type: application/json" \
  -d "{
    \"txHash\": \"$TX_HASH_2\",
    \"amount\": 2000000.000000,
    \"depositedAt\": \"$(date -u +'%Y-%m-%dT%H:%M:%S')\",
    \"operator\": \"treasury_dept_002\",
    \"remark\": \"Second collateral deposit\"
  }" | jq '.'
echo ""
echo ""

# 测试 8: 查询所有审计报告
echo "[测试 8] 查询所有审计报告"
echo "GET /api/audit/reports"
curl -s -X GET "$BASE_URL/api/audit/reports" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# 测试 9: 手动生成审计报告
echo "[测试 9] 手动生成审计报告（过去1小时）"
echo "POST /api/audit/reports/generate"
START_TIME=$(date -u -d '1 hour ago' +'%Y-%m-%dT%H:%M:%S')
END_TIME=$(date -u +'%Y-%m-%dT%H:%M:%S')
curl -s -X POST "$BASE_URL/api/audit/reports/generate" \
  -H "Content-Type: application/json" \
  -d "{
    \"start\": \"$START_TIME\",
    \"end\": \"$END_TIME\",
    \"operator\": \"test-manual-trigger\"
  }" | jq '.'
echo ""
echo ""

# 测试 10: 查询储备率历史
echo "[测试 10] 查询储备率历史（过去2小时）"
echo "GET /api/reserve/history"
START_TIME=$(date -u -d '2 hours ago' +'%Y-%m-%dT%H:%M:%S')
END_TIME=$(date -u +'%Y-%m-%dT%H:%M:%S')
curl -s -X GET "$BASE_URL/api/reserve/history?start=$START_TIME&end=$END_TIME" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# 测试 11: 查询风险告警
echo "[测试 11] 查询风险告警"
echo "GET /api/risk/alerts"
curl -s -X GET "$BASE_URL/api/risk/alerts" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

# 测试 12: 验证储备是否支持发行（模拟课题1调用）
echo "[测试 12] 验证储备是否支持发行（假设流通量 2000000）"
echo "POST /api/reserve/check?stablecoinSupply=2000000"
curl -s -X POST "$BASE_URL/api/reserve/check?stablecoinSupply=2000000" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo ""

echo "=========================================="
echo "测试完成"
echo "=========================================="
