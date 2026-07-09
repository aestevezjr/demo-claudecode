import { FormArray } from '@angular/forms';

export interface InvoiceTotals {
  subtotal: number;
  taxAmount: number;
  total: number;
}

/**
 * Calcula el total de una línea de factura (cantidad * precio unitario).
 * Función pura: no modifica estado externo.
 */
export function computeLineTotal(quantity: number | string, unitPrice: number | string): number {
  const qty = parseFloat(String(quantity)) || 0;
  const price = parseFloat(String(unitPrice)) || 0;
  return qty * price;
}

/**
 * Calcula subtotal, impuesto y total a partir de un FormArray de ítems y una tasa de impuesto.
 * Función pura: solo lee valores, no modifica el formulario.
 */
export function computeInvoiceTotals(itemsArray: FormArray, taxRate: number | string): InvoiceTotals {
  const subtotal = itemsArray.controls.reduce((sum, _, i) => {
    const item = itemsArray.at(i).value;
    return sum + computeLineTotal(item.quantity, item.unitPrice);
  }, 0);

  const rate = parseFloat(String(taxRate)) || 0;
  const taxAmount = subtotal * rate / 100;
  const total = subtotal + taxAmount;

  return { subtotal, taxAmount, total };
}

/**
 * Formatea un objeto Date o string ISO a 'YYYY-MM-DD'.
 * Función pura: no produce efectos secundarios.
 */
export function formatDateToISO(date: Date | string): string {
  const d = date instanceof Date ? date : new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
