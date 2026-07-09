# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack invoice management application (FacturasApp):
- **Frontend**: Angular 17.3 + Angular Material 17 (`/frontend`)
- **Backend**: Spring Boot 3.4.1 / Java 17 + Maven (`/backend`)
- **Database**: MySQL — schema at `database/schema.sql`

## Prerequisites & First-Time Setup

Before starting the backend, run the schema manually in your MySQL client:
```
mysql -u root -p < database/schema.sql
```

Update MySQL credentials in `backend/src/main/resources/application.properties` if your password differs from `root`.

## Commands

### Backend
```bash
# Run (from /backend)
mvn spring-boot:run

# Compile only
mvn compile

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=InvoiceServiceImplTest
```

### Frontend
```bash
# Dev server at http://localhost:4200 (from /frontend)
ng serve

# Build
ng build --configuration development
ng build  # production

# Run unit tests
ng test

# Run a single spec file
ng test --include="**/invoice-list.component.spec.ts"
```

## Architecture

### Backend layers (`backend/src/main/java/com/facturas/api/`)

| Layer | Package | Responsibility |
|-------|---------|---------------|
| Controller | `controller/` | HTTP endpoints, request/response mapping |
| Service | `service/impl/` | Business logic, total computation, number generation |
| Repository | `repository/` | JPA queries — `InvoiceRepository` extends `JpaRepository` |
| DTOs | `dto/` | `InvoiceRequestDTO` (inbound), `InvoiceResponseDTO` (outbound), `InvoiceItemDTO` (both) |
| Entities | `entity/` | `Invoice` + `InvoiceItem` (JPA, no MapStruct — manual mapping in `InvoiceServiceImpl`) |

**Key design decisions:**
- `invoice_number` is **auto-generated** by the backend (`FAC-YYYY-NNNN`); it is NOT part of `InvoiceRequestDTO`. Never add it there.
- `subtotal`, `taxAmount`, and `total` are **always computed by the backend** in `computeTotals()` — never trust values sent from the frontend.
- `@OneToMany(cascade=ALL, orphanRemoval=true)` on `Invoice.items`: on update, the service calls `invoice.getItems().clear()` then re-adds all items. This requires `@Transactional` on the update method — do not remove it.
- All monetary values use `BigDecimal` with `RoundingMode.HALF_UP`. Never use `double`/`float`.
- `ddl-auto=validate` — Hibernate validates the schema on startup. Schema changes require updating `database/schema.sql` and re-running it manually.

### Frontend structure (`frontend/src/app/`)

```
app.routes.ts          — lazy loadComponent routes for all features
app.config.ts          — provideRouter, provideHttpClient, provideAnimationsAsync
core/
  models/invoice.model.ts    — TypeScript interfaces (Invoice, InvoiceItem, InvoiceStatus)
  services/invoice.service.ts — HTTP calls to /api/v1/invoices
  services/theme.service.ts  — light/dark theme toggle, persisted in localStorage
shared/components/     — ConfirmDialogComponent, StatusBadgeComponent
features/invoices/
  invoice-list/        — mat-table, delete via MatDialog
  invoice-form/        — create + edit (same component, detects mode via route :id param)
  invoice-detail/      — read-only view + PDF export via html2canvas + jsPDF
```

**Key frontend patterns:**
- All routes use **lazy `loadComponent`** — never import feature components eagerly in `app.config.ts`.
- `InvoiceFormComponent` is reused for create (`/invoices/new`) and edit (`/invoices/:id/edit`). It detects mode by checking `route.snapshot.paramMap.get('id')`.
- The form uses a **`FormArray`** for line items. Use `asFormGroup(control)` helper to cast before accessing nested controls in the template.
- Totals (subtotal, tax, total) are **computed client-side in real-time** via `valueChanges` subscription for preview — the authoritative values are always what the backend returns.
- The **dark theme** applies by toggling `.dark-theme` on `document.body`. Custom color overrides for non-Material elements live in `styles.scss` under `body.dark-theme { ... }`.
- Angular Material theme is defined as **custom SCSS** (not a prebuilt CSS file) in `styles.scss` — this is required for the dual light/dark theme to work. Do not re-add a prebuilt theme to `angular.json`.

### API contract

Base URL (hardcoded in `InvoiceService`): `http://localhost:8080/api/v1/invoices`

| Method | Path | Notes |
|--------|------|-------|
| GET | `/` | Returns all invoices ordered by date desc |
| GET | `/:id` | Single invoice with items |
| POST | `/` | Creates; `invoiceNumber` is server-generated |
| PUT | `/:id` | Full update; items are replaced entirely |
| DELETE | `/:id` | Cascades to `invoice_items` |
| PATCH | `/:id/status` | Body is a raw JSON string: `"PAID"` |

CORS is configured in `config/CorsConfig.java` to allow `http://localhost:4200` only.

### PDF export

`InvoiceDetailComponent.exportPDF()` uses `html2canvas` to capture `#invoiceDoc` (the `<mat-card>` marked with `#invoiceDoc` template ref) and converts it to A4 PDF via `jsPDF`. The export always uses a white background — dark theme does not affect the PDF output.
