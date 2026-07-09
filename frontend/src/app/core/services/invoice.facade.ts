import { Injectable, signal, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { InvoiceService } from './invoice.service';
import { Invoice, InvoiceStatus, PageResponse } from '../models/invoice.model';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';

/**
 * Facade que centraliza toda la lógica de estado y operaciones CRUD de facturas.
 * Los componentes interactuan exclusivamente con esta capa; nunca con InvoiceService directamente.
 *
 * Responsabilidades:
 * - Mantener el estado local de la lista (signals)
 * - Orquestar las llamadas HTTP a través de InvoiceService
 * - Gestionar el dialog de confirmacion de eliminacion
 * - Emitir notificaciones (snackbar) sobre el resultado de las operaciones
 */
@Injectable({ providedIn: 'root' })
export class InvoiceFacade {
  private readonly invoiceService = inject(InvoiceService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  // --- Estado reactivo con signals ---
  private readonly _invoices      = signal<Invoice[]>([]);
  private readonly _isLoading     = signal(false);
  private readonly _currentPage   = signal(0);
  private readonly _pageSize      = signal(5);
  private readonly _totalElements = signal(0);
  private readonly _totalPages    = signal(0);

  /** Lista de facturas de la página actual (solo lectura desde los componentes). */
  readonly invoices      = this._invoices.asReadonly();

  /** Indica si hay una operacion de carga en curso. */
  readonly isLoading     = this._isLoading.asReadonly();

  readonly currentPage   = this._currentPage.asReadonly();
  readonly pageSize      = this._pageSize.asReadonly();
  readonly totalElements = this._totalElements.asReadonly();
  readonly totalPages    = this._totalPages.asReadonly();

  /** Numero total de facturas en el servidor. */
  readonly invoiceCount = computed(() => this._totalElements());

  // --- Operaciones de lectura ---

  /** Carga una página de facturas desde el backend y actualiza el estado. */
  loadPage(page: number, size: number): void {
    this._isLoading.set(true);
    this._currentPage.set(page);
    this._pageSize.set(size);
    this.invoiceService.getPage(page, size).subscribe({
      next: (data: PageResponse<Invoice>) => {
        this._invoices.set(data.content);
        this._totalElements.set(data.totalElements);
        this._totalPages.set(data.totalPages);
        this._isLoading.set(false);
      },
      error: () => {
        this.showError('Error al cargar las facturas');
        this._isLoading.set(false);
      },
    });
  }

  /**
   * Obtiene una factura por ID. Devuelve el Observable directamente
   * porque el componente de detalle necesita el dato puntual sin mantenerlo en lista.
   */
  getById(id: number): Observable<Invoice> {
    return this.invoiceService.getById(id);
  }

  // --- Operaciones de escritura ---

  /** Crea una nueva factura y navega al detalle al completarse. */
  create(invoice: Invoice): void {
    this.invoiceService.create(invoice).subscribe({
      next: (saved) => {
        this.showSuccess('Factura creada');
        this.router.navigate(['/invoices', saved.id]);
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Error al guardar la factura';
        this.showError(msg);
      },
    });
  }

  /** Actualiza una factura existente y navega al detalle al completarse. */
  update(id: number, invoice: Invoice): void {
    this.invoiceService.update(id, invoice).subscribe({
      next: (saved) => {
        this.showSuccess('Factura actualizada');
        this.router.navigate(['/invoices', saved.id]);
      },
      error: (err) => {
        const msg = err.error?.message ?? 'Error al guardar la factura';
        this.showError(msg);
      },
    });
  }

  /** Actualiza el estado de una factura sin recargar la lista completa. */
  updateStatus(id: number, status: InvoiceStatus): void {
    this.invoiceService.updateStatus(id, status).subscribe({
      next: (updated) => {
        this._invoices.update(list =>
          list.map(inv => inv.id === updated.id ? updated : inv)
        );
        this.showSuccess('Estado actualizado');
      },
      error: () => this.showError('Error al actualizar el estado'),
    });
  }

  /**
   * Muestra un dialog de confirmacion y, si el usuario acepta, elimina la factura.
   * Tras la eliminacion actualiza la lista local sin recargar desde el backend.
   * Si se proporciona `navigateAfter`, redirige a esa ruta.
   */
  confirmAndDelete(invoice: Invoice, navigateAfter?: string[]): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Eliminar factura',
        message: `¿Estás seguro de que deseas eliminar la factura ${invoice.invoiceNumber}?`,
      },
      width: '400px',
    });

    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed && invoice.id) {
        this.invoiceService.delete(invoice.id).subscribe({
          next: () => {
            this.showSuccess('Factura eliminada');
            if (navigateAfter) {
              this.router.navigate(navigateAfter);
            } else {
              // Si era el último ítem de una página no-inicial, retroceder una página
              const targetPage = this._invoices().length === 1 && this._currentPage() > 0
                ? this._currentPage() - 1
                : this._currentPage();
              this.loadPage(targetPage, this._pageSize());
            }
          },
          error: () => this.showError('Error al eliminar la factura'),
        });
      }
    });
  }

  // --- Navegacion ---

  navigateTo(commands: unknown[]): void {
    this.router.navigate(commands);
  }

  // --- Helpers privados ---

  private showSuccess(message: string): void {
    this.snackBar.open(message, 'Cerrar', { duration: 3000 });
  }

  private showError(message: string): void {
    this.snackBar.open(message, 'Cerrar', { duration: 4000 });
  }
}
