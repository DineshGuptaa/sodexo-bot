
-- MySQL 8.0+ script for the Data Governance SQL practical
-- Creates a sample database, tables, data, and runs the required queries

-- 1) Create database
DROP DATABASE IF EXISTS dg_practical;
CREATE DATABASE dg_practical;
USE dg_practical;

-- 2) Create tables
CREATE TABLE transactions (
  txn_id      BIGINT PRIMARY KEY,
  account_id  BIGINT NOT NULL,
  txn_ts      TIMESTAMP NOT NULL,
  amount      DECIMAL(18,2) NULL,
  currency    VARCHAR(3) NOT NULL
);

CREATE TABLE ops_totals (
  `date` DATE PRIMARY KEY,
  total_amount DECIMAL(18,2) NOT NULL
);

-- 3) Insert sample data
-- Accounts 101, 102; include near-duplicate txns within 2 minutes for detection
INSERT INTO transactions (txn_id, account_id, txn_ts, amount, currency) VALUES
  (1, 101, '2026-02-20 09:00:00', 100.00, 'USD'),
  (2, 101, '2026-02-20 09:01:30', 100.00, 'USD'), -- duplicate within 90 seconds
  (3, 101, '2026-02-20 09:10:00', 250.00, 'USD'),
  (4, 101, '2026-02-21 11:00:00', 300.00, 'USD'),
  (5, 102, '2026-02-21 11:00:30', 300.00, 'USD'), -- different account -> not dup for 101
  (6, 102, '2026-02-21 12:00:00', NULL   , 'USD'), -- NULL amount for completeness calc
  (7, 102, '2026-02-22 08:00:00',  50.00, 'EUR'),
  (8, 102, '2026-02-22 08:01:50',  50.00, 'EUR'), -- duplicate within 110 seconds
  (9, 102, '2026-02-22 08:05:00',  75.00, 'EUR'),
  (10,101, '2026-02-15 08:00:00',  10.00, 'USD');  -- outside 7-day window

-- ops_totals for reconciliation (daily totals we expect from Ops)
INSERT INTO ops_totals (`date`, total_amount) VALUES
  ('2026-02-20', 350.00), -- actual calc will be 100 + 100 + 250 = 450.00 (variance present)
  ('2026-02-21', 600.00), -- actual calc will be 300 + 300 + NULL = 600.00 (matches when ignoring NULL)
  ('2026-02-22', 175.00); -- actual calc will be 50 + 50 + 75 = 175.00 (matches)

-- 4) Required Queries

-- Q1: Find potential duplicates within 2 minutes for same account, amount, currency
WITH w AS (
  SELECT 
    t.*, 
    LAG(txn_ts) OVER (PARTITION BY account_id, amount, currency ORDER BY txn_ts) AS prev_ts
  FROM transactions t
)
SELECT *
FROM w
WHERE prev_ts IS NOT NULL
  AND TIMESTAMPDIFF(SECOND, prev_ts, txn_ts) <= 120;

-- Q2: Completeness: % rows with non-NULL amount in last 7 days
SELECT 
  ROUND(100.0 * SUM(CASE WHEN amount IS NOT NULL THEN 1 ELSE 0 END) / COUNT(*), 2) AS pct_non_null
FROM transactions
WHERE txn_ts >= NOW() - INTERVAL 7 DAY;

-- Q3: Reconcile daily totals vs ops_totals and show variances > 0.5%
WITH t AS (
  SELECT DATE(txn_ts) AS dt, SUM(COALESCE(amount, 0)) AS calc_total
  FROM transactions
  GROUP BY 1
)
SELECT 
  t.dt, 
  t.calc_total, 
  o.total_amount,
  ROUND(100.0 * (t.calc_total - o.total_amount) / NULLIF(o.total_amount, 0), 4) AS variance_pct
FROM t 
JOIN ops_totals o ON o.date = t.dt
WHERE ABS(100.0 * (t.calc_total - o.total_amount) / NULLIF(o.total_amount, 0)) > 0.5
ORDER BY t.dt;
