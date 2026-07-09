# FacturasApp

Aplicación web full-stack para gestión de facturas con ítems de línea, cálculo automático de totales, estados y exportación a PDF.

## Stack Tecnológico

| Capa | Tecnología | Versión |
|------|-----------|---------|
| Frontend | Angular + TypeScript | 17.3.17 |
| UI | Angular Material | 17 |
| Backend | Spring Boot + Java | 3.4.1 / Java 17 |
| Persistencia | Spring Data JPA + Hibernate | incluido en Boot |
| Base de datos | MySQL | 8+ |
| Build backend | Maven | 3.6+ |
| PDF export | html2canvas + jsPDF | — |

## Funcionalidades

- CRUD completo de facturas con ítems de línea
- Número de factura autogenerado (`FAC-YYYY-NNNN`)
- Totales calculados en el servidor con `BigDecimal` (subtotal, impuesto, total)
- Preview de totales en tiempo real en el formulario (client-side)
- Estados: `PENDING` / `PAID` / `CANCELLED`
- Tema claro / oscuro con persistencia en `localStorage`
- Exportación de factura a PDF (A4) mediante html2canvas + jsPDF

## Prerrequisitos

- Java 17+
- Maven 3.6+
- Node.js 18+ y Angular CLI 17 (`npm install -g @angular/cli@17`)
- MySQL 8+ corriendo en `localhost:3306`

## Configuración Inicial

### 1. Base de datos

Ejecuta el schema una sola vez en tu cliente MySQL:

```bash
mysql -u root -p < database/schema.sql
```

Esto crea la base de datos `facturas_db` y las tablas `invoices` e `invoice_items`.

### 2. Credenciales MySQL

Si tu contraseña difiere de `root`, edita `backend/src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=TU_PASSWORD
```

## Levantar la Aplicación

### Backend (puerto 8080)

```bash
cd backend
mvn spring-boot:run
```

### Frontend (puerto 4200)

```bash
cd frontend
ng serve
```

Abre `http://localhost:4200` en el navegador.

## Comandos Útiles

### Backend

```bash
mvn compile                          # Compilar sin ejecutar
mvn test                             # Correr todos los tests
mvn test -Dtest=InvoiceServiceImplTest  # Correr un test específico
```

### Frontend

```bash
ng build                             # Build de producción
ng build --configuration development # Build de desarrollo
ng test                              # Correr todos los tests
ng test --include="**/invoice-list.component.spec.ts"  # Test específico
```

## Estructura del Proyecto

```
demo-claudecode/
├── database/
│   └── schema.sql                  # DDL — correr manualmente antes del primer inicio
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/facturas/api/
│       ├── entity/                 # Invoice, InvoiceItem (JPA)
│       ├── enums/                  # InvoiceStatus
│       ├── repository/             # JpaRepository + queries personalizados
│       ├── dto/                    # InvoiceRequestDTO, InvoiceResponseDTO, InvoiceItemDTO
│       ├── service/impl/           # InvoiceServiceImpl — lógica de negocio y totales
│       ├── controller/             # InvoiceController — endpoints REST
│       ├── config/                 # CorsConfig (permite localhost:4200)
│       └── exception/              # GlobalExceptionHandler, ResourceNotFoundException
└── frontend/
    └── src/app/
        ├── app.routes.ts           # Rutas lazy (loadComponent)
        ├── app.config.ts           # Providers globales
        ├── core/
        │   ├── models/             # invoice.model.ts — interfaces TypeScript
        │   └── services/           # invoice.service.ts, theme.service.ts
        ├── shared/components/      # ConfirmDialogComponent, StatusBadgeComponent
        └── features/invoices/
            ├── invoice-list/       # Tabla con mat-table
            ├── invoice-form/       # Formulario create + edit (ReactiveForm + FormArray)
            └── invoice-detail/     # Vista read-only + exportar PDF
```

## API REST

Base URL: `http://localhost:8080/api/v1/invoices`

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/` | Lista todas las facturas ordenadas por fecha desc |
| GET | `/:id` | Obtiene una factura con sus ítems |
| POST | `/` | Crea una factura; el número se genera en el servidor |
| PUT | `/:id` | Actualiza una factura completa (reemplaza todos los ítems) |
| DELETE | `/:id` | Elimina la factura y sus ítems en cascada |
| PATCH | `/:id/status` | Actualiza solo el estado (body: string JSON crudo, ej. `"PAID"`) |

### Ejemplo: Crear factura

```bash
curl -X POST http://localhost:8080/api/v1/invoices \
  -H "Content-Type: application/json" \
  -d '{
    "issueDate": "2026-06-20",
    "clientName": "Empresa ABC",
    "clientEmail": "admin@abc.com",
    "clientAddress": "Av. Principal 123",
    "taxRate": 16,
    "items": [
      { "description": "Consultoría", "quantity": 10, "unitPrice": 150.00 }
    ]
  }'
```

Respuesta `201 Created` con `invoiceNumber: "FAC-2026-0001"`, `subtotal: 1500.00`, `taxAmount: 240.00`, `total: 1740.00`.

## Base de Datos

### Tabla `invoices`

| Columna | Tipo | Notas |
|---------|------|-------|
| `id` | BIGINT PK | Auto-increment |
| `invoice_number` | VARCHAR(50) UNIQUE | Generado por el backend |
| `issue_date` | DATE | |
| `status` | ENUM | `PENDING`, `PAID`, `CANCELLED` |
| `client_name` | VARCHAR(255) | |
| `client_email` | VARCHAR(255) | |
| `client_address` | VARCHAR(500) | |
| `subtotal` | DECIMAL(12,2) | Calculado por el backend |
| `tax_rate` | DECIMAL(5,2) | % de impuesto |
| `tax_amount` | DECIMAL(12,2) | Calculado por el backend |
| `total` | DECIMAL(12,2) | Calculado por el backend |
| `notes` | TEXT | Opcional |
| `created_at` / `updated_at` | DATETIME | Automáticos |

### Tabla `invoice_items`

| Columna | Tipo | Notas |
|---------|------|-------|
| `id` | BIGINT PK | Auto-increment |
| `invoice_id` | BIGINT FK | `ON DELETE CASCADE` |
| `description` | VARCHAR(500) | |
| `quantity` | DECIMAL(10,3) | |
| `unit_price` | DECIMAL(12,2) | |
| `line_total` | DECIMAL(12,2) | `quantity × unit_price`, calculado |
| `sort_order` | INT | Orden de aparición en la factura |

## Decisiones de Diseño

- **`invoiceNumber` nunca viene del cliente** — el campo no existe en `InvoiceRequestDTO`; se genera en `InvoiceServiceImpl.generateInvoiceNumber()` al crear
- **Totales siempre del servidor** — `computeTotals()` recalcula `lineTotal`, `subtotal`, `taxAmount` y `total` en cada escritura; los valores del frontend son solo preview
- **Actualización de ítems** — el update llama `invoice.getItems().clear()` y re-agrega los ítems del DTO; requiere `@OneToMany(orphanRemoval=true)` y `@Transactional`
- **Dinero con `BigDecimal`** — nunca se usa `double`/`float`; todos los redondeos son `RoundingMode.HALF_UP`
- **Schema gestionado manualmente** — `ddl-auto=validate`: Hibernate verifica pero no modifica el schema; los cambios deben hacerse en `database/schema.sql` y re-ejecutarse
- **Tema personalizado en SCSS** — no se usa el CSS prebuilt de Angular Material; el tema se define con `@use '@angular/material' as mat` para soportar el toggle claro/oscuro en runtime
