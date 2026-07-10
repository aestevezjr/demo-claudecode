import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { InvoiceFacade } from '../../../core/services/invoice.facade';
import { Invoice } from '../../../core/models/invoice.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { PaginatorComponent, PageChangeEvent } from '../../../shared/components/paginator/paginator.component';

@Component({
  selector: 'app-invoice-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    StatusBadgeComponent,
    CurrencyPipe,
    DatePipe,
    PaginatorComponent,
  ],
  templateUrl: './invoice-list.component.html',
  styleUrls: ['./invoice-list.component.scss'],
})
export class InvoiceListComponent implements OnInit, OnDestroy {
  readonly invoices      = this.facade.invoices;
  readonly isLoading     = this.facade.isLoading;
  readonly currentPage   = this.facade.currentPage;
  readonly pageSize      = this.facade.pageSize;
  readonly totalPages    = this.facade.totalPages;
  readonly totalElements = this.facade.totalElements;
  readonly searchTerm    = this.facade.searchTerm;

  readonly pageSizeOptions = [5, 10, 15];

  private readonly searchSubject = new Subject<string>();
  private searchSub!: Subscription;

  constructor(readonly facade: InvoiceFacade) {}

  ngOnInit(): void {
    this.facade.loadPage(0, 5);
    this.searchSub = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
    ).subscribe(term => this.facade.search(term));
  }

  ngOnDestroy(): void {
    this.searchSub.unsubscribe();
  }

  onSearch(term: string): void {
    this.searchSubject.next(term);
  }

  onPageChange(event: PageChangeEvent): void {
    this.facade.loadPage(event.page, event.size);
  }

  viewInvoice(id: number): void {
    this.facade.navigateTo(['/invoices', id]);
  }

  editInvoice(id: number): void {
    this.facade.navigateTo(['/invoices', id, 'edit']);
  }

  deleteInvoice(invoice: Invoice): void {
    this.facade.confirmAndDelete(invoice);
  }
}
