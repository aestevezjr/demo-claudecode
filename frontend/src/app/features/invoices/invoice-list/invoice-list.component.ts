import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormGroup, FormControl } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDividerModule } from '@angular/material/divider';
import { MatRadioModule } from '@angular/material/radio';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { provideNativeDateAdapter } from '@angular/material/core';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { InvoiceFacade } from '../../../core/services/invoice.facade';
import { Invoice, InvoiceStatus } from '../../../core/models/invoice.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { PaginatorComponent, PageChangeEvent } from '../../../shared/components/paginator/paginator.component';

@Component({
  selector: 'app-invoice-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [provideNativeDateAdapter()],
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatDividerModule,
    MatRadioModule,
    MatDatepickerModule,
    StatusBadgeComponent,
    CurrencyPipe,
    DatePipe,
    PaginatorComponent,
  ],
  templateUrl: './invoice-list.component.html',
  styleUrls: ['./invoice-list.component.scss'],
})
export class InvoiceListComponent implements OnInit, OnDestroy {
  readonly invoices          = this.facade.invoices;
  readonly isLoading         = this.facade.isLoading;
  readonly currentPage       = this.facade.currentPage;
  readonly pageSize          = this.facade.pageSize;
  readonly totalPages        = this.facade.totalPages;
  readonly totalElements     = this.facade.totalElements;
  readonly searchTerm        = this.facade.searchTerm;
  readonly statusFilter      = this.facade.statusFilter;
  readonly activeFilterCount = this.facade.activeFilterCount;

  readonly sidebarOpen     = signal(true);
  readonly pageSizeOptions = [5, 10, 15];

  readonly STATUSES: InvoiceStatus[] = ['PENDING', 'PAID', 'CANCELLED'];
  readonly STATUS_LABELS: Record<InvoiceStatus, string> = {
    PENDING: 'Pendiente',
    PAID: 'Pagada',
    CANCELLED: 'Cancelada',
  };

  readonly filterForm = new FormGroup({
    dateFrom:  new FormControl<Date | null>(null),
    dateTo:    new FormControl<Date | null>(null),
    minAmount: new FormControl<number | null>(null),
    maxAmount: new FormControl<number | null>(null),
  });

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$      = new Subject<void>();
  private searchSub!: Subscription;

  constructor(readonly facade: InvoiceFacade) {}

  ngOnInit(): void {
    this.facade.loadPage(0, 5);

    this.searchSub = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
    ).subscribe(term => this.facade.search(term));

    this.filterForm.controls.dateFrom.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(v =>
        this.facade.setDateRange(
          this.toIsoDate(v),
          this.toIsoDate(this.filterForm.controls.dateTo.value),
        )
      );

    this.filterForm.controls.dateTo.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(v =>
        this.facade.setDateRange(
          this.toIsoDate(this.filterForm.controls.dateFrom.value),
          this.toIsoDate(v),
        )
      );

    this.filterForm.controls.minAmount.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(v =>
        this.facade.setAmountRange(v ?? null, this.filterForm.controls.maxAmount.value ?? null)
      );

    this.filterForm.controls.maxAmount.valueChanges
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(v =>
        this.facade.setAmountRange(this.filterForm.controls.minAmount.value ?? null, v ?? null)
      );
  }

  ngOnDestroy(): void {
    this.searchSub.unsubscribe();
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleSidebar(): void { this.sidebarOpen.update(v => !v); }

  onSearch(term: string): void { this.searchSubject.next(term); }

  setStatus(status: InvoiceStatus | null): void { this.facade.setStatus(status); }

  clearFilters(): void {
    this.filterForm.reset(undefined, { emitEvent: false });
    this.facade.clearFilters();
  }

  onPageChange(event: PageChangeEvent): void { this.facade.loadPage(event.page, event.size); }

  viewInvoice(id: number): void  { this.facade.navigateTo(['/invoices', id]); }

  editInvoice(id: number): void  { this.facade.navigateTo(['/invoices', id, 'edit']); }

  deleteInvoice(invoice: Invoice): void { this.facade.confirmAndDelete(invoice); }

  private toIsoDate(d: Date | null | undefined): string | null {
    if (!d) return null;
    const m   = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${m}-${day}`;
  }
}
