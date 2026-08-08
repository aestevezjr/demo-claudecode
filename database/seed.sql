-- ============================================================
-- Seed Data — 30 facturas de prueba (PostgreSQL / Supabase)
-- ============================================================

-- ─── FACTURAS ────────────────────────────────────────────────
INSERT INTO invoices
  (id, invoice_number, issue_date, status, client_name, client_email, client_address,
   subtotal, tax_rate, tax_amount, total, notes, created_at, updated_at)
VALUES
-- 2024 (10 facturas)
(1,  'FAC-2024-0001', '2024-01-15', 'PAID',      'Acme Corp',                  'contacto@acmecorp.co',          'Cra 15 #93-75, Bogotá',               3300.00, 19.00,  627.00,  3927.00, 'Proyecto completado en plazo.',                   '2024-01-15 09:00:00', '2024-02-01 10:00:00'),
(2,  'FAC-2024-0002', '2024-01-28', 'PAID',      'Constructora El Pino',        'info@elpino.com',               'Av. El Dorado #68-35, Bogotá',         2550.00,  0.00,    0.00,  2550.00, NULL,                                              '2024-01-28 10:00:00', '2024-02-15 11:00:00'),
(3,  'FAC-2024-0003', '2024-02-10', 'CANCELLED', 'TechSolutions S.A.S',         'tech@solutions.co',             'Cl. 100 #8A-55, Bogotá',              1100.00, 19.00,  209.00,  1309.00, 'Cancelado por cambio de proveedor.',              '2024-02-10 08:30:00', '2024-02-20 14:00:00'),
(4,  'FAC-2024-0004', '2024-03-05', 'PAID',      'Distribuidora Norte',         'ventas@dnorte.com',             'Cl. 50 #20-30, Medellín',               800.00,  5.00,   40.00,   840.00, NULL,                                              '2024-03-05 11:00:00', '2024-03-20 09:00:00'),
(5,  'FAC-2024-0005', '2024-04-20', 'PENDING',   'Grupo Empresarial Andino',    'gea@andino.com',                'Carrera 7 #71-21, Bogotá',             4300.00, 19.00,  817.00,  5117.00, 'Pendiente de aprobación por gerencia.',           '2024-04-20 14:00:00', '2024-04-20 14:00:00'),
(6,  'FAC-2024-0006', '2024-05-12', 'PAID',      'Clínica Santa Fe',            'clinica@santafe.co',            'Cl. 119 #7-75, Bogotá',               2800.00,  0.00,    0.00,  2800.00, NULL,                                              '2024-05-12 09:00:00', '2024-06-01 10:00:00'),
(7,  'FAC-2024-0007', '2024-06-30', 'PAID',      'Restaurante El Jardín',       'eljardins@rest.co',             'Cl. 82 #11-37, Bogotá',               2200.00, 19.00,  418.00,  2618.00, NULL,                                              '2024-06-30 10:30:00', '2024-07-15 09:00:00'),
(8,  'FAC-2024-0008', '2024-08-14', 'CANCELLED', 'Importadora Pacific',         'imports@pacific.co',            'Zona Franca L3, Cartagena',           2000.00, 19.00,  380.00,  2380.00, 'Cancelado por problemas aduaneros.',              '2024-08-14 08:00:00', '2024-08-30 16:00:00'),
(9,  'FAC-2024-0009', '2024-09-22', 'PAID',      'Editorial Planeta Colombia',  'editorial@planeta.co',          'Cra 11A #98-50, Bogotá',              1575.00,  0.00,    0.00,  1575.00, NULL,                                              '2024-09-22 11:00:00', '2024-10-10 09:00:00'),
(10, 'FAC-2024-0010', '2024-11-08', 'PENDING',   'Ingeniería Civil S.A.',       'civil@ingenieria.co',           'Av. 68 #23-50, Bogotá',               9500.00, 19.00, 1805.00, 11305.00, 'En revisión por el departamento técnico.',        '2024-11-08 09:00:00', '2024-11-08 09:00:00'),

-- 2025 (15 facturas)
(11, 'FAC-2025-0001', '2025-01-10', 'PAID',      'Seguros Bolívar',             'seguros@bolivar.co',            'Cl. 26 #68C-60, Bogotá',              4200.00, 19.00,  798.00,  4998.00, NULL,                                              '2025-01-10 09:00:00', '2025-01-28 10:00:00'),
(12, 'FAC-2025-0002', '2025-01-25', 'PAID',      'Farmacéutica Lafrancol',      'info@lafrancol.com',            'Cll 9A #34-70, Cali',                 1800.00,  0.00,    0.00,  1800.00, NULL,                                              '2025-01-25 10:00:00', '2025-02-10 11:00:00'),
(13, 'FAC-2025-0003', '2025-02-14', 'PENDING',   'Banco de Bogotá S.A.',        'banco@bogota.co',               'Cl. 36 #7-47, Bogotá',                6600.00, 19.00, 1254.00,  7854.00, 'Sujeto a revisión legal.',                        '2025-02-14 08:00:00', '2025-02-14 08:00:00'),
(14, 'FAC-2025-0004', '2025-02-28', 'PAID',      'Café de Colombia Export',     'cafe@colombia.co',              'Cl. 73 #8-13, Bogotá',                2900.00,  0.00,    0.00,  2900.00, NULL,                                              '2025-02-28 11:00:00', '2025-03-15 09:00:00'),
(15, 'FAC-2025-0005', '2025-03-17', 'PAID',      'Industrias Haceb',            'compras@haceb.com',             'Cl. 52 #48-42, Medellín',            11500.00, 19.00, 2185.00, 13685.00, 'Incluye soporte técnico por 12 meses.',           '2025-03-17 09:00:00', '2025-04-05 10:00:00'),
(16, 'FAC-2025-0006', '2025-04-05', 'CANCELLED', 'Aerolíneas Latam',            'pagos@latam.com',               'El Dorado T2, Bogotá',                5000.00, 19.00,  950.00,  5950.00, 'Cancelado por recorte de presupuesto.',           '2025-04-05 08:00:00', '2025-04-20 15:00:00'),
(17, 'FAC-2025-0007', '2025-04-22', 'PAID',      'Universidad de los Andes',    'uniandes@edu.co',               'Cl. 19A #1-37 Este, Bogotá',          7300.00,  0.00,    0.00,  7300.00, 'Proyecto de investigación semestral.',            '2025-04-22 10:00:00', '2025-05-10 09:00:00'),
(18, 'FAC-2025-0008', '2025-05-10', 'PENDING',   'Supermercados Jumbo',         'proveedores@jumbo.co',          'Autopista Norte Km 8, Bogotá',         6400.00, 19.00, 1216.00,  7616.00, 'Servicio de mantenimiento preventivo.',           '2025-05-10 09:00:00', '2025-05-10 09:00:00'),
(19, 'FAC-2025-0009', '2025-06-01', 'PAID',      'EPM Empresas Públicas',       'epm@epm.com.co',                'Cl. 30 #43-81, Medellín',             9500.00, 19.00, 1805.00, 11305.00, NULL,                                              '2025-06-01 08:00:00', '2025-06-20 10:00:00'),
(20, 'FAC-2025-0010', '2025-07-14', 'PAID',      'Grupo Nutresa',               'nutresa@nutresa.com',           'Cl. 44 #91-07, Medellín',             7000.00,  0.00,    0.00,  7000.00, NULL,                                              '2025-07-14 09:00:00', '2025-08-01 10:00:00'),
(21, 'FAC-2025-0011', '2025-08-20', 'PENDING',   'Claro Colombia',              'pagos@claro.com.co',            'Cra 68 #26-57, Bogotá',              18000.00, 19.00, 3420.00, 21420.00, 'Proyecto infraestructura crítica.',               '2025-08-20 08:00:00', '2025-08-20 08:00:00'),
(22, 'FAC-2025-0012', '2025-09-05', 'PAID',      'Bancolombia S.A.',            'bancolombia@bancolombia.com',   'Cl. 29 #7-18, Medellín',             17500.00, 19.00, 3325.00, 20825.00, NULL,                                              '2025-09-05 09:00:00', '2025-09-25 10:00:00'),
(23, 'FAC-2025-0013', '2025-09-28', 'PAID',      'Avianca Holdings',            'avianca@avianca.com',           'Av. El Dorado, Bogotá',              11000.00, 19.00, 2090.00, 13090.00, NULL,                                              '2025-09-28 10:00:00', '2025-10-15 09:00:00'),
(24, 'FAC-2025-0014', '2025-10-15', 'CANCELLED', 'Cementos Argos',              'argos@argos.com.co',            'Cl. 7 #43B-79, Medellín',            10500.00,  0.00,    0.00, 10500.00, 'Suspendido por cambio regulatorio.',              '2025-10-15 08:00:00', '2025-10-30 14:00:00'),
(25, 'FAC-2025-0015', '2025-11-30', 'PENDING',   'Coca-Cola FEMSA',             'femsa@femsa.com',               'Cra 7 #156-78, Bogotá',              18000.00, 19.00, 3420.00, 21420.00, 'Campaña de fin de año.',                          '2025-11-30 09:00:00', '2025-11-30 09:00:00'),

-- 2026 (5 facturas)
(26, 'FAC-2026-0001', '2026-01-08', 'PAID',      'Rappi Colombia',              'rappi@rappi.com',               'Cl. 92 #11-51, Bogotá',              10000.00, 19.00, 1900.00, 11900.00, NULL,                                              '2026-01-08 09:00:00', '2026-01-25 10:00:00'),
(27, 'FAC-2026-0002', '2026-02-14', 'PENDING',   'Postobón S.A.',               'postobon@postobon.co',          'Cra 63A #14-30, Bogotá',              3600.00, 19.00,  684.00,  4284.00, 'Renovación de imagen corporativa.',               '2026-02-14 10:00:00', '2026-02-14 10:00:00'),
(28, 'FAC-2026-0003', '2026-03-22', 'PAID',      'Fundación Santa Fe',          'fsantafe@fsantafe.org',         'Cl. 116 #9-02, Bogotá',              17400.00,  0.00,    0.00, 17400.00, 'Exento de IVA — entidad sin ánimo de lucro.',    '2026-03-22 08:00:00', '2026-04-10 09:00:00'),
(29, 'FAC-2026-0004', '2026-05-10', 'PENDING',   'Mineros S.A.',                'mineros@mineros.com.co',        'Cl. 7 #43B-79, Medellín',             7300.00, 19.00, 1387.00,  8687.00, NULL,                                              '2026-05-10 09:00:00', '2026-05-10 09:00:00'),
(30, 'FAC-2026-0005', '2026-06-30', 'PENDING',   'Davivienda S.A.',             'davivienda@davivienda.com',     'Cl. 29 #7-18, Bogotá',               33000.00, 19.00, 6270.00, 39270.00, 'Contrato anual de ciberseguridad.',               '2026-06-30 09:00:00', '2026-06-30 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- Reset sequence so future inserts don't collide with seeded IDs
SELECT setval('invoices_id_seq', (SELECT MAX(id) FROM invoices));

-- ─── ITEMS ───────────────────────────────────────────────────
INSERT INTO invoice_items (invoice_id, description, quantity, unit_price, line_total, sort_order) VALUES
-- FAC-2024-0001
(1,  'Desarrollo web',                           1.000,   2500.00,  2500.00, 0),
(1,  'Diseño UI/UX',                             1.000,    800.00,   800.00, 1),
-- FAC-2024-0002
(2,  'Consultoría técnica',                      3.000,    450.00,  1350.00, 0),
(2,  'Informe estructural',                      1.000,   1200.00,  1200.00, 1),
-- FAC-2024-0003
(3,  'Licencia software',                        2.000,    350.00,   700.00, 0),
(3,  'Soporte técnico',                          5.000,     80.00,   400.00, 1),
-- FAC-2024-0004
(4,  'Transporte mercancía',                     1.000,    650.00,   650.00, 0),
(4,  'Embalaje',                               100.000,      1.50,   150.00, 1),
-- FAC-2024-0005
(5,  'Auditoría contable',                       1.000,   3500.00,  3500.00, 0),
(5,  'Declaración de renta',                     1.000,    800.00,   800.00, 1),
-- FAC-2024-0006
(6,  'Mantenimiento equipos médicos',            4.000,    250.00,  1000.00, 0),
(6,  'Repuestos especializados',                 1.000,   1800.00,  1800.00, 1),
-- FAC-2024-0007
(7,  'Diseño de menú',                           1.000,    400.00,   400.00, 0),
(7,  'Fotografía gastronómica',                  1.000,   1200.00,  1200.00, 1),
(7,  'Gestión redes sociales',                   1.000,    600.00,   600.00, 2),
-- FAC-2024-0008
(8,  'Trámites aduaneros',                       1.000,   2000.00,  2000.00, 0),
-- FAC-2024-0009
(9,  'Corrección de estilo (págs.)',           250.000,      3.50,   875.00, 0),
(9,  'Diseño de portada',                        1.000,    700.00,   700.00, 1),
-- FAC-2024-0010
(10, 'Estudio de suelos',                        1.000,   4500.00,  4500.00, 0),
(10, 'Levantamiento topográfico',                1.000,   2000.00,  2000.00, 1),
(10, 'Planos estructurales',                     1.000,   3000.00,  3000.00, 2),
-- FAC-2025-0001
(11, 'Consultoría de riesgo',                    2.000,   1800.00,  3600.00, 0),
(11, 'Informe técnico ejecutivo',                1.000,    600.00,   600.00, 1),
-- FAC-2025-0002
(12, 'Análisis de laboratorio',                 10.000,     85.00,   850.00, 0),
(12, 'Informe de certificación',                 1.000,    950.00,   950.00, 1),
-- FAC-2025-0003
(13, 'Consultoría regulatoria',                  3.000,   2200.00,  6600.00, 0),
-- FAC-2025-0004
(14, 'Diseño material exportación',              1.000,   1500.00,  1500.00, 0),
(14, 'Certificados de calidad',                  5.000,    120.00,   600.00, 1),
(14, 'Logística de envíos',                      1.000,    800.00,   800.00, 2),
-- FAC-2025-0005
(15, 'Software ERP módulo principal',            1.000,   8500.00,  8500.00, 0),
(15, 'Instalación y configuración',              1.000,   1500.00,  1500.00, 1),
(15, 'Capacitación de usuarios',                 2.000,    750.00,  1500.00, 2),
-- FAC-2025-0006
(16, 'Campaña publicidad digital',               1.000,   5000.00,  5000.00, 0),
-- FAC-2025-0007
(17, 'Investigación aplicada',                   1.000,   7000.00,  7000.00, 0),
(17, 'Publicación de artículo científico',       1.000,    300.00,   300.00, 1),
-- FAC-2025-0008
(18, 'Mantenimiento preventivo refrigeración',  12.000,    350.00,  4200.00, 0),
(18, 'Repuestos especializados',                 1.000,   2200.00,  2200.00, 1),
-- FAC-2025-0009
(19, 'Consultoría energética',                   5.000,   1200.00,  6000.00, 0),
(19, 'Software de monitoreo',                    1.000,   3500.00,  3500.00, 1),
-- FAC-2025-0010
(20, 'Auditoría de calidad',                     1.000,   4200.00,  4200.00, 0),
(20, 'Certificación ISO 9001',                   1.000,   2800.00,  2800.00, 1),
-- FAC-2025-0011
(21, 'Instalación fibra óptica',                 1.000,  15000.00, 15000.00, 0),
(21, 'Configuración de red',                     1.000,   3000.00,  3000.00, 1),
-- FAC-2025-0012
(22, 'Diseño app móvil iOS/Android',             1.000,  12000.00, 12000.00, 0),
(22, 'Pruebas QA y testing',                     1.000,   4000.00,  4000.00, 1),
(22, 'Documentación técnica',                    1.000,   1500.00,  1500.00, 2),
-- FAC-2025-0013
(23, 'Análisis de datos',                        1.000,   6500.00,  6500.00, 0),
(23, 'Desarrollo dashboard BI',                  1.000,   4500.00,  4500.00, 1),
-- FAC-2025-0014
(24, 'Estudio de impacto ambiental',             1.000,   8000.00,  8000.00, 0),
(24, 'Gestión de permisos legales',              1.000,   2500.00,  2500.00, 1),
-- FAC-2025-0015
(25, 'Marketing digital campaña',                1.000,   9000.00,  9000.00, 0),
(25, 'Producción video corporativo',             1.000,   5500.00,  5500.00, 1),
(25, 'Pauta en medios online',                   1.000,   3500.00,  3500.00, 2),
-- FAC-2026-0001
(26, 'Desarrollo API REST',                      1.000,   7500.00,  7500.00, 0),
(26, 'Testing y control de calidad',             1.000,   2500.00,  2500.00, 1),
-- FAC-2026-0002
(27, 'Rediseño de empaque',                      3.000,    800.00,  2400.00, 0),
(27, 'Registro de marca',                        1.000,   1200.00,  1200.00, 1),
-- FAC-2026-0003
(28, 'Sistema de gestión de pacientes',          1.000,  15000.00, 15000.00, 0),
(28, 'Capacitación del personal',                2.000,   1200.00,  2400.00, 1),
-- FAC-2026-0004
(29, 'Auditoría de seguridad informática',       1.000,   5500.00,  5500.00, 0),
(29, 'Informe técnico detallado',                2.000,    900.00,  1800.00, 1),
-- FAC-2026-0005
(30, 'Consultoría en ciberseguridad',            1.000,  20000.00, 20000.00, 0),
(30, 'Implementación de políticas de seguridad', 1.000,   5000.00,  5000.00, 1),
(30, 'Pruebas de penetración (pentest)',          1.000,   8000.00,  8000.00, 2)
ON CONFLICT DO NOTHING;

-- Reset sequence for invoice_items
SELECT setval('invoice_items_id_seq', (SELECT MAX(id) FROM invoice_items));

-- ─── SECUENCIAS ──────────────────────────────────────────────
INSERT INTO invoice_sequences (invoice_year, last_seq)
VALUES (2024, 10), (2025, 15), (2026, 5)
ON CONFLICT (invoice_year) DO NOTHING;
