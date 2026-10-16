#!/usr/bin/env bash
# Seeds sample invoices into a LOCAL backend (in-memory H2) for screenshots/demos.
# Start the backend in demo mode first (see "Demo mode" in README.md).
set -euo pipefail
API=${API:-http://localhost:8082}
DEMO_USER=${DEMO_USER:-demo}
DEMO_PASSWORD=${DEMO_PASSWORD:-Demo-de0a21efe32a}   # local demo-only credentials
JAR=$(mktemp)
trap 'rm -f "$JAR"' EXIT

curl -s -c "$JAR" -b "$JAR" -o /dev/null "$API/auth/me" || true
xsrf() { awk '$6=="XSRF-TOKEN"{print $7}' "$JAR" | tail -1; }
# Body goes through stdin so non-ASCII text (accents, dashes) survives on Windows.
post() { printf '%s' "$3" | curl -s -f -c "$JAR" -b "$JAR" -H "Content-Type: application/json; charset=utf-8" -H "X-XSRF-TOKEN: $(xsrf)" -X "$1" "$API$2" --data-binary @-; }

post POST /auth/register "{\"username\":\"$DEMO_USER\",\"password\":\"$DEMO_PASSWORD\"}" >/dev/null || echo "user exists, continuing"
post POST /auth/login    "{\"username\":\"$DEMO_USER\",\"password\":\"$DEMO_PASSWORD\"}" >/dev/null
curl -s -c "$JAR" -b "$JAR" -o /dev/null "$API/auth/me"

invoice() { post POST /api/v1/invoices "$1" >/dev/null && echo "created: $(echo "$1" | sed -E 's/.*"clientName":"([^"]+)".*/\1/')"; }

invoice '{"issueDate":"2026-07-03","status":"PAID","clientName":"Caribe Logistics SRL","clientEmail":"cuentas@caribelogistics.example","clientAddress":"Av. Winston Churchill 1099, Santo Domingo","taxRate":18,"notes":"Thank you for your business.","items":[{"description":"Route optimization module — development","quantity":40,"unitPrice":55},{"description":"Integration with fleet GPS API","quantity":16,"unitPrice":60}]}'
invoice '{"issueDate":"2026-07-21","status":"PAID","clientName":"Ferretería Las Américas","clientEmail":"admin@lasamericas.example","clientAddress":"Calle El Conde 254, Santo Domingo","taxRate":18,"items":[{"description":"Inventory web app — maintenance (monthly)","quantity":1,"unitPrice":850},{"description":"Database backup automation","quantity":6,"unitPrice":45}]}'
invoice '{"issueDate":"2026-08-08","status":"PENDING","clientName":"Clínica San Rafael","clientEmail":"finanzas@clinicasanrafael.example","clientAddress":"Av. Abraham Lincoln 702, Santo Domingo","taxRate":18,"notes":"Net 30.","items":[{"description":"Patient appointment portal — sprint 3","quantity":60,"unitPrice":52.5},{"description":"Security review and hardening","quantity":12,"unitPrice":70}]}'
invoice '{"issueDate":"2026-08-19","status":"PAID","clientName":"Andes Coffee Co.","clientEmail":"billing@andescoffee.example","clientAddress":"Carrera 7 #71-21, Bogotá","taxRate":0,"notes":"Export of services — 0% tax.","items":[{"description":"E-commerce checkout redesign","quantity":1,"unitPrice":2400},{"description":"Payment gateway integration","quantity":1,"unitPrice":950}]}'
invoice '{"issueDate":"2026-09-02","status":"CANCELLED","clientName":"Distribuidora del Este","clientEmail":"compras@deleste.example","clientAddress":"Calle Duarte 18, La Romana","taxRate":18,"notes":"Cancelled at client request; replaced by FAC-2026-0007.","items":[{"description":"Sales reporting dashboard","quantity":24,"unitPrice":50}]}'
invoice '{"issueDate":"2026-09-15","status":"PENDING","clientName":"Banco Mercantil Digital","clientEmail":"proveedores@bmdigital.example","clientAddress":"Av. 27 de Febrero 450, Santo Domingo","taxRate":18,"items":[{"description":"REST API for account statements","quantity":80,"unitPrice":65},{"description":"Load testing and performance report","quantity":10,"unitPrice":70},{"description":"Technical documentation","quantity":8,"unitPrice":40}]}'
invoice '{"issueDate":"2026-09-30","status":"PAID","clientName":"Distribuidora del Este","clientEmail":"compras@deleste.example","clientAddress":"Calle Duarte 18, La Romana","taxRate":18,"items":[{"description":"Sales reporting dashboard (revised scope)","quantity":18,"unitPrice":50},{"description":"Excel export feature","quantity":4,"unitPrice":50}]}'
invoice '{"issueDate":"2026-10-06","status":"PENDING","clientName":"Hotel Playa Dorada","clientEmail":"it@playadorada.example","clientAddress":"Playa Dorada, Puerto Plata","taxRate":18,"notes":"Includes on-site training.","items":[{"description":"Booking engine integration","quantity":30,"unitPrice":58},{"description":"Staff training session","quantity":2,"unitPrice":200}]}'
echo "Done. Log in at http://localhost:4200 as '$DEMO_USER'."
