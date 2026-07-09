-- ============================================================
-- Invoice App - MySQL Schema
-- Run this script before starting the Spring Boot backend
-- ============================================================

CREATE DATABASE IF NOT EXISTS facturas_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE facturas_db;

CREATE TABLE IF NOT EXISTS invoices (
  id              BIGINT          NOT NULL AUTO_INCREMENT,
  invoice_number  VARCHAR(50)     NOT NULL,
  issue_date      DATE            NOT NULL,
  status          ENUM('PENDING','PAID','CANCELLED') NOT NULL DEFAULT 'PENDING',
  client_name     VARCHAR(255)    NOT NULL,
  client_email    VARCHAR(255)    NOT NULL,
  client_address  VARCHAR(500)    NOT NULL,
  subtotal        DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
  tax_rate        DECIMAL(5, 2)   NOT NULL DEFAULT 0.00,
  tax_amount      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
  total           DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
  notes           TEXT,
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_invoice_number (invoice_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice_items (
  id           BIGINT          NOT NULL AUTO_INCREMENT,
  invoice_id   BIGINT          NOT NULL,
  description  VARCHAR(500)    NOT NULL,
  quantity     DECIMAL(10, 3)  NOT NULL DEFAULT 1.000,
  unit_price   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
  line_total   DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
  sort_order   INT             NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT fk_items_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_invoices_status       ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoices_issue_date   ON invoices(issue_date);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id);

CREATE TABLE IF NOT EXISTS users (
  id        BIGINT       NOT NULL AUTO_INCREMENT,
  username  VARCHAR(50)  NOT NULL,
  password  VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice_sequences (
  invoice_year  INT    NOT NULL,
  last_seq      BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (invoice_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
