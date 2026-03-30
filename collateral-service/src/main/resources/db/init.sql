-- ============================================================
-- Stablecoin Collateral Management System - Database Init SQL
-- Database: PostgreSQL
-- ============================================================

CREATE DATABASE collateral_db;
\c collateral_db;

-- 1. 鎶垫娂鐗╁瓨鍏ヨ褰曡〃
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

-- 2. 鍌ㄥ姹犵姸鎬佽〃锛堝崟琛岃仛鍚堬級
CREATE TABLE IF NOT EXISTS reserve_pool (
    id                BIGSERIAL PRIMARY KEY,
    total_usd_amount  NUMERIC(20, 6) NOT NULL DEFAULT 0,
    locked_amount     NUMERIC(20, 6) NOT NULL DEFAULT 0,
    stablecoin_supply NUMERIC(20, 6) NOT NULL DEFAULT 0,
    last_updated      TIMESTAMP NOT NULL DEFAULT NOW()
);
INSERT INTO reserve_pool (total_usd_amount, locked_amount, stablecoin_supply) VALUES (0, 0, 0);
-- 升级已有数据库：补充 stablecoin_supply 列
ALTER TABLE reserve_pool ADD COLUMN IF NOT EXISTS stablecoin_supply NUMERIC(20, 6) NOT NULL DEFAULT 0;

-- 3. 鍌ㄥ鐜囧揩鐓ц〃锛堟椂搴忥級
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

-- 4. 椋庨櫓璀︽姤琛?
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

-- 5. 瀹¤鎶ュ憡琛?
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

-- 6. 姹囩巼璁板綍琛?
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

-- 7. 鍌ㄥ璧勪骇閰嶇疆琛紙鏂板锛?
-- 椋庨櫓绛夌骇锛? = LOW, 2 = MEDIUM, 3 = HIGH
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

-- 鍒濆鍖栧偍澶囪祫浜э紙鍑忓皯鍒濆閲戦锛屾敼涓烘洿鍚堢悊鐨勮妯★級
-- 鎬诲偍澶囷細1000 涓?USD锛堣€屼笉鏄?1035 浜匡級
INSERT INTO reserve_assets (asset_type, asset_name, usd_value, target_allocation_ratio, risk_level, annual_yield, description) VALUES
    ('US_TREASURY', '缇庡浗鐭湡鍥藉€?, 8400000.000000, 0.840000, 1, 0.053000, '缇庡浗鏀垮簻鐭湡鍊哄埜锛屽畨鍏ㄦ€ф渶楂?),
    ('MONEY_MARKET', '璐у竵甯傚満鍩洪噾', 400000.000000, 0.040000, 1, 0.052000, '楂樻祦鍔ㄦ€ц揣甯佸熀閲?),
    ('OVERNIGHT_REPO', '闅斿閫嗗洖璐崗璁?, 800000.000000, 0.080000, 1, 0.054000, '闅斿閫嗗洖璐崗璁紝娴佸姩鎬у己'),
    ('CASH', '鐜伴噾鍙婇摱琛屽瓨娆?, 100000.000000, 0.010000, 1, 0.001000, '娲绘湡瀛樻鍜岀幇閲?),
    ('GOLD', '瀹炵墿榛勯噾', 400000.000000, 0.040000, 2, 0.000000, '瀹炵墿榛勯噾鍌ㄥ'),
    ('BITCOIN', '姣旂壒甯?, 200000.000000, 0.020000, 3, 0.000000, '姣旂壒甯佽祫浜?),
    ('OTHER', '鍏朵粬璧勪骇', 100000.000000, 0.010000, 2, 0.030000, '鍏朵粬鎶曡祫璧勪骇');

-- 鏇存柊鍌ㄥ姹犲垵濮嬪€间负 1000 涓?USD
UPDATE reserve_pool SET total_usd_amount = 10000000.000000 WHERE id = 1;

