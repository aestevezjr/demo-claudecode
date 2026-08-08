-- ============================================================
-- Invoice App - PostgreSQL / Supabase Schema
-- Run this script in Supabase SQL Editor before starting the backend
-- ============================================================

-- Trigger function to auto-update updated_at on every UPDATE
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS invoices (
  id              BIGSERIAL       PRIMARY KEY,
  invoice_number  VARCHAR(50)     NOT NULL,
  issue_date      DATE            NOT NULL,
  status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
  client_name     VARCHAR(255)    NOT NULL,
  client_email    VARCHAR(255)    NOT NULL,
  client_address  VARCHAR(500)    NOT NULL,
  subtotal        DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
  tax_rate        DECIMAL(5,  2)  NOT NULL DEFAULT 0.00,
  tax_amount      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
  total           DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
  notes           TEXT,
  created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
  CONSTRAINT uk_invoice_number UNIQUE (invoice_number)
);

CREATE OR REPLACE TRIGGER trg_invoices_updated_at
  BEFORE UPDATE ON invoices
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS invoice_items (
  id           BIGSERIAL       PRIMARY KEY,
  invoice_id   BIGINT          NOT NULL REFERENCES invoices(id) ON DELETE CASCADE ON UPDATE CASCADE,
  description  VARCHAR(500)    NOT NULL,
  quantity     DECIMAL(10, 3)  NOT NULL DEFAULT 1.000,
  unit_price   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
  line_total   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
  sort_order   INT             NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS invoice_sequences (
  invoice_year  INT    NOT NULL PRIMARY KEY,
  last_seq      BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS users (
  id             BIGSERIAL      PRIMARY KEY,
  username       VARCHAR(50)    NOT NULL,
  password       VARCHAR(255)   NOT NULL,
  role           VARCHAR(20)    NOT NULL DEFAULT 'USER',
  token_version  INT            NOT NULL DEFAULT 0,
  CONSTRAINT uk_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_invoices_status        ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoices_issue_date    ON invoices(issue_date);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice  ON invoice_items(invoice_id);
