-- ============================================================================
-- Migration: 002_add_missing_not_null_columns.sql
-- Purpose:   Fix three more schema drifts caused by the same root cause as 001:
--            Hibernate's ddl-auto=update tried to ADD NOT NULL columns to tables
--            that already contained rows, PostgreSQL refused ("contains null
--            values"), and the columns were never created - so every startup
--            re-logged the error and the affected queries failed at runtime
--            (e.g. GET /api/prescriptions -> "column duration_days does not exist").
--
-- Affected columns (all match the current JPA entities):
--   prescription_items.times_per_day  (int, NOT NULL, entity default 1)
--   prescription_items.duration_days  (int, NOT NULL, entity default 1)
--   inventory_batches.unit_price      (numeric(10,2), NOT NULL, entity field)
--
-- Each column is added with a DEFAULT so PostgreSQL backfills existing rows,
-- then NOT NULL is enforced. All statements are idempotent.
-- ============================================================================

-- 1. prescription_items.times_per_day (entity: int timesPerDay = 1)
ALTER TABLE prescription_items ADD COLUMN IF NOT EXISTS times_per_day integer NOT NULL DEFAULT 1;

-- 2. prescription_items.duration_days (entity: int durationDays = 1)
ALTER TABLE prescription_items ADD COLUMN IF NOT EXISTS duration_days integer NOT NULL DEFAULT 1;

-- 3. inventory_batches.unit_price (entity: BigDecimal unitPrice, numeric(10,2))
--    Add as nullable with a default, backfill existing batches to their unit cost
--    (list price = cost for legacy rows), then enforce NOT NULL.
ALTER TABLE inventory_batches ADD COLUMN IF NOT EXISTS unit_price numeric(10,2) DEFAULT 0;
UPDATE inventory_batches SET unit_price = unit_cost WHERE unit_price IS NULL OR unit_price = 0;
ALTER TABLE inventory_batches ALTER COLUMN unit_price SET NOT NULL;
