import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';
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
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatCardModule,
    StatusBadgeComponent,
    CurrencyPipe,
    DatePipe,
    PaginatorComponent,
  ],
  templateUrl: './invoice-list.component.html',
  styleUrls: ['./invoice-list.component.scss'],
})
export class InvoiceListComponent implements OnInit {
  readonly invoices      = this.facade.invoices;
  readonly isLoading     = this.facade.isLoading;
  readonly currentPage   = this.facade.currentPage;
  readonly pageSize      = this.facade.pageSize;
  readonly totalPages    = this.facade.totalPages;
  readonly totalElements = this.facade.totalElements;

  readonly pageSizeOptions = [5, 10, 15];

  constructor(private readonly facade: InvoiceFacade) {}

  ngOnInit(): void {
    this.facade.loadPage(0, 5);
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
