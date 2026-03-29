-- Reserve Assets Table (Tether Q4 2024 Attestation Report)
CREATE TABLE IF NOT EXISTS reserve_assets (
    id               BIGSERIAL PRIMARY KEY,
    asset_type       VARCHAR(30)    NOT NULL UNIQUE,
    asset_name       VARCHAR(100)   NOT NULL,
    usd_value        NUMERIC(24, 6) NOT NULL DEFAULT 0,
    allocation_ratio NUMERIC(8, 6)  NOT NULL DEFAULT 0,
    risk_level       VARCHAR(10)    NOT NULL DEFAULT 'LOW',
    annual_yield     NUMERIC(8, 6)  DEFAULT 0,
    description      TEXT,
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed data based on Tether Q4 2024 actual allocation
INSERT INTO reserve_assets
    (asset_type, asset_name, usd_value, allocation_ratio, risk_level, annual_yield, description)
VALUES
    ('US_TREASURY',    'US Treasury Bills',       84000000000.000000, 0.840000, 'LOW',    0.053000, 'Primary reserve: short-term US Treasury Bills, 7th largest US debt holder in 2024'),
    ('OVERNIGHT_REPO', 'Overnight Repo',            8000000000.000000, 0.080000, 'LOW',    0.054000, 'Overnight reverse repurchase agreements collateralized by US Treasuries'),
    ('MONEY_MARKET',   'Money Market Funds',        4000000000.000000, 0.040000, 'LOW',    0.052000, 'Money market funds investing in US government securities'),
    ('GOLD',           'Physical Gold',             4000000000.000000, 0.040000, 'MEDIUM', 0.000000, 'Physical gold reserves, added to portfolio in 2023'),
    ('CASH',           'Cash & Bank Deposits',      1000000000.000000, 0.010000, 'LOW',    0.001000, 'Cash held in bank accounts, minimal proportion'),
    ('BITCOIN',        'Bitcoin',                   2000000000.000000, 0.020000, 'HIGH',   0.000000, 'Bitcoin added to reserves from profits in 2023, ~2% allocation'),
    ('OTHER',          'Other Assets',               500000000.000000, 0.005000, 'MEDIUM', 0.030000, 'Corporate bonds, loans and other miscellaneous assets')
ON CONFLICT (asset_type) DO NOTHING;
