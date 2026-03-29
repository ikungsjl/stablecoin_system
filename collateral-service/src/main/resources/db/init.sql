-- ============================================================
-- Stablecoin Collateral Management System - Database Init SQL
-- Database: PostgreSQL
-- ============================================================

CREATE DATABASE collateral_db;
\c collateral_db;

-- 1. 抵押物存入记录表
CREATE TABLE IF NOT EXISTS collateral_deposits (
    id              BIGSERIAL PRIMARY KEY,
    tx_hash         VARCHAR(66)  NOT NULL UNIQUE,
    amount          NUMERIC(20, 6) NOT NULL,
    currency        VARCHAR(10)  NOT NULL DEFAULT 'USD',
    usd_amount      NUMERIC(20, 6) NOT NULL,
    exchange_rate   NUMERIC(10, 6) NOT NULL DEFAULT 1.0,
    operator        VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED',
    remark          TEXT,
    deposited_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_collateral_deposits_deposited_at ON collateral_deposits(deposited_at);
CREATE INDEX idx_collateral_deposits_status       ON collateral_deposits(status);

-- 2. 储备池状态表（单行聚合）
CREATE TABLE IF NOT EXISTS reserve_pool (
    id               BIGSERIAL PRIMARY KEY,
    total_usd_amount NUMERIC(20, 6) NOT NULL DEFAULT 0,
    locked_amount    NUMERIC(20, 6) NOT NULL DEFAULT 0,
    last_updated     TIMESTAMP NOT NULL DEFAULT NOW()
);
INSERT INTO reserve_pool (total_usd_amount, locked_amount) VALUES (0, 0);

-- 3. 储备率快照表（时序）
CREATE TABLE IF NOT EXISTS reserve_snapshots (
    id                BIGSERIAL PRIMARY KEY,
    reserve_amount    NUMERIC(20, 6) NOT NULL,
    stablecoin_supply NUMERIC(20, 6) NOT NULL,
    reserve_ratio     NUMERIC(10, 6) NOT NULL,
    risk_level        VARCHAR(20)    NOT NULL,
    snapshot_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reserve_snapshots_snapshot_at ON reserve_snapshots(snapshot_at);
CREATE INDEX idx_reserve_snapshots_risk_level  ON reserve_snapshots(risk_level);

-- 4. 风险警报表
CREATE TABLE IF NOT EXISTS risk_alerts (
    id            BIGSERIAL PRIMARY KEY,
    alert_type    VARCHAR(50)    NOT NULL,
    risk_level    VARCHAR(20)    NOT NULL,
    reserve_ratio NUMERIC(10, 6),
    gap_amount    NUMERIC(20, 6),
    description   TEXT           NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    resolved_by   VARCHAR(100),
    resolved_at   TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_risk_alerts_status     ON risk_alerts(status);
CREATE INDEX idx_risk_alerts_created_at ON risk_alerts(created_at);

-- 5. 审计报告表
CREATE TABLE IF NOT EXISTS audit_reports (
    id               BIGSERIAL PRIMARY KEY,
    report_no        VARCHAR(50)    NOT NULL UNIQUE,
    period_start     TIMESTAMP NOT NULL,
    period_end       TIMESTAMP NOT NULL,
    avg_ratio        NUMERIC(10, 6),
    min_ratio        NUMERIC(10, 6),
    max_ratio        NUMERIC(10, 6),
    total_deposit    NUMERIC(20, 6),
    total_issuance   NUMERIC(20, 6),
    total_redemption NUMERIC(20, 6),
    alert_count      INTEGER DEFAULT 0,
    report_data      TEXT,
    generated_by     VARCHAR(100),
    generated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_reports_period ON audit_reports(period_start, period_end);

-- 6. 汇率记录表
CREATE TABLE IF NOT EXISTS exchange_rates (
    id          BIGSERIAL PRIMARY KEY,
    currency    VARCHAR(10)    NOT NULL,
    rate_to_usd NUMERIC(10, 6) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_exchange_rates_currency ON exchange_rates(currency);
INSERT INTO exchange_rates (currency, rate_to_usd) VALUES
    ('USD', 1.000000),
    ('CNY', 0.137900),
    ('EUR', 1.085000);

-- 7. 储备资产配置表（新增）
-- 风险等级：1 = LOW, 2 = MEDIUM, 3 = HIGH
CREATE TABLE IF NOT EXISTS reserve_assets (
    id                      BIGSERIAL PRIMARY KEY,
    asset_type              VARCHAR(30)    NOT NULL UNIQUE,
    asset_name              VARCHAR(100)   NOT NULL,
    usd_value               NUMERIC(20, 6) NOT NULL,
    target_allocation_ratio NUMERIC(8, 6)  NOT NULL,
    risk_level              INTEGER        NOT NULL,
    annual_yield            NUMERIC(8, 6),
    description             TEXT,
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reserve_assets_risk_level ON reserve_assets(risk_level);

-- 初始化储备资产（减少初始金额，改为更合理的规模）
-- 总储备：1000 万 USD（而不是 1035 亿）
INSERT INTO reserve_assets (asset_type, asset_name, usd_value, target_allocation_ratio, risk_level, annual_yield, description) VALUES
    ('US_TREASURY', '美国短期国债', 8400000.000000, 0.840000, 1, 0.053000, '美国政府短期债券，安全性最高'),
    ('MONEY_MARKET', '货币市场基金', 400000.000000, 0.040000, 1, 0.052000, '高流动性货币基金'),
    ('OVERNIGHT_REPO', '隔夜逆回购协议', 800000.000000, 0.080000, 1, 0.054000, '隔夜逆回购协议，流动性强'),
    ('CASH', '现金及银行存款', 100000.000000, 0.010000, 1, 0.001000, '活期存款和现金'),
    ('GOLD', '实物黄金', 400000.000000, 0.040000, 2, 0.000000, '实物黄金储备'),
    ('BITCOIN', '比特币', 200000.000000, 0.020000, 3, 0.000000, '比特币资产'),
    ('OTHER', '其他资产', 100000.000000, 0.010000, 2, 0.030000, '其他投资资产');

-- 更新储备池初始值为 1000 万 USD
UPDATE reserve_pool SET total_usd_amount = 10000000.000000 WHERE id = 1;
