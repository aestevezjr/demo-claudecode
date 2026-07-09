import { Component, ElementRef, OnInit, ViewChild, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { InvoiceFacade } from '../../../core/services/invoice.facade';
import { Invoice } from '../../../core/models/invoice.model';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

@Component({
  selector: 'app-invoice-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatDividerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    StatusBadgeComponent,
    CurrencyPipe,
    DatePipe,
  ],
  templateUrl: './invoice-detail.component.html',
  styleUrls: ['./invoice-detail.component.scss'],
})
export class InvoiceDetailComponent implements OnInit {
  @ViewChild('invoiceDoc', { read: ElementRef }) invoiceDocRef!: ElementRef<HTMLElement>;

  invoice: Invoice | null = null;
  isLoading = true;
  isExporting = false;
  readonly displayedColumns = ['description', 'quantity', 'unitPrice', 'lineTotal'];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly facade: InvoiceFacade,
    private readonly snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.facade.getById(id).subscribe({
      next: (data) => {
        this.invoice = data;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.snackBar.open('Factura no encontrada', 'Cerrar', { duration: 3000 });
        this.facade.navigateTo(['/invoices']);
      },
    });
  }

  async exportPDF(): Promise<void> {
    if (!this.invoice || !this.invoiceDocRef) return;
    this.isExporting = true;
    this.cdr.markForCheck();

    try {
      const el = this.invoiceDocRef.nativeElement;
      const canvas = await html2canvas(el, {
        scale: 2,
        useCORS: true,
        backgroundColor: '#ffffff',
      });

      const imgData = canvas.toDataURL('image/png');
      const pdf = new jsPDF('p', 'mm', 'a4');
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = (canvas.height * pdfWidth) / canvas.width;

      pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
      pdf.save(`${this.invoice.invoiceNumber}.pdf`);
    } catch (err) {
      console.error('[PDF Export] Error:', err);
      this.snackBar.open('Error al generar el PDF', 'Cerrar', { duration: 3000 });
    } finally {
      this.isExporting = false;
      this.cdr.markForCheck();
    }
  }

  delete(): void {
    if (!this.invoice) return;
    this.facade.confirmAndDelete(this.invoice, ['/invoices']);
  }
}
