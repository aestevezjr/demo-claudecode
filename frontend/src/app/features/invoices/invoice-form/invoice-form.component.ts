import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { InvoiceFacade } from '../../../core/services/invoice.facade';
import { Invoice, InvoiceItem, InvoiceStatus } from '../../../core/models/invoice.model';
import {
  computeInvoiceTotals,
  computeLineTotal,
  formatDateToISO,
  InvoiceTotals,
} from '../../../core/utils/invoice.utils';

@Component({
  selector: 'app-invoice-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatDividerModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './invoice-form.component.html',
  styleUrls: ['./invoice-form.component.scss'],
})
export class InvoiceFormComponent implements OnInit {
  form!: FormGroup;
  isEditMode = false;
  invoiceId: number | null = null;
  isLoading = false;
  isSaving = false;
  invoiceNumber = '';

  readonly statusOptions: { value: InvoiceStatus; label: string }[] = [
    { value: 'PENDING', label: 'Pendiente' },
    { value: 'PAID', label: 'Pagada' },
    { value: 'CANCELLED', label: 'Cancelada' },
  ];

  totals: InvoiceTotals = { subtotal: 0, taxAmount: 0, total: 0 };

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly facade: InvoiceFacade,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.buildForm();

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.invoiceId = +id;
      this.loadInvoice(this.invoiceId);
    } else {
      this.addItem();
    }

    this.form.valueChanges.subscribe(() => {
      this.totals = computeInvoiceTotals(this.itemsArray, this.form.get('taxRate')?.value);
      this.cdr.markForCheck();
    });
  }

  // --- Construccion del formulario ---

  private buildForm(): void {
    this.form = this.fb.group({
      issueDate: [new Date(), Validators.required],
      status: ['PENDING'],
      clientName: ['', Validators.required],
      clientEmail: ['', [Validators.required, Validators.email]],
      clientAddress: ['', Validators.required],
      taxRate: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
      notes: [''],
      items: this.fb.array([]),
    });
  }

  get itemsArray(): FormArray {
    return this.form.get('items') as FormArray;
  }

  private createItemGroup(item?: InvoiceItem): FormGroup {
    return this.fb.group({
      id: [item?.id ?? null],
      description: [item?.description ?? '', Validators.required],
      quantity: [item?.quantity ?? 1, [Validators.required, Validators.min(0.001)]],
      unitPrice: [item?.unitPrice ?? 0, [Validators.required, Validators.min(0)]],
    });
  }

  // --- Gestion de items ---

  addItem(): void {
    this.itemsArray.push(this.createItemGroup());
  }

  removeItem(index: number): void {
    if (this.itemsArray.length > 1) {
      this.itemsArray.removeAt(index);
      this.totals = computeInvoiceTotals(this.itemsArray, this.form.get('taxRate')?.value);
    }
  }

  lineTotal(index: number): number {
    const item = this.itemsArray.at(index).value;
    return computeLineTotal(item.quantity, item.unitPrice);
  }

  // --- Carga de datos en modo edicion ---

  private loadInvoice(id: number): void {
    this.isLoading = true;
    this.facade.getById(id).subscribe({
      next: (invoice) => {
        this.invoiceNumber = invoice.invoiceNumber;
        while (this.itemsArray.length) {
          this.itemsArray.removeAt(0);
        }
        invoice.items.forEach(item => this.itemsArray.push(this.createItemGroup(item)));

        this.form.patchValue({
          issueDate: new Date(invoice.issueDate),
          status: invoice.status,
          clientName: invoice.clientName,
          clientEmail: invoice.clientEmail,
          clientAddress: invoice.clientAddress,
          taxRate: invoice.taxRate,
          notes: invoice.notes ?? '',
        });

        this.totals = computeInvoiceTotals(this.itemsArray, invoice.taxRate);
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.facade.navigateTo(['/invoices']);
      },
    });
  }

  // --- Validacion y mensajes de error ---

  getFieldError(field: string): string {
    const control = this.form.get(field);
    if (control?.hasError('required')) return 'Este campo es requerido';
    if (control?.hasError('email')) return 'Email inválido';
    if (control?.hasError('min')) return 'El valor es demasiado bajo';
    if (control?.hasError('max')) return 'El valor es demasiado alto';
    return '';
  }

  getItemError(index: number, field: string): string {
    const control = this.itemsArray.at(index).get(field);
    if (control?.hasError('required')) return 'Requerido';
    if (control?.hasError('min')) return 'Valor inválido';
    return '';
  }

  asFormGroup(control: AbstractControl): FormGroup {
    return control as FormGroup;
  }

  // --- Envio del formulario ---

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const formValue = this.form.value;
    const invoice: Invoice = {
      ...formValue,
      issueDate: formatDateToISO(formValue.issueDate),
      items: formValue.items,
    };

    this.isSaving = true;

    if (this.isEditMode && this.invoiceId) {
      this.facade.update(this.invoiceId, invoice);
    } else {
      this.facade.create(invoice);
    }
  }

  cancel(): void {
    const route = this.invoiceId ? ['/invoices', this.invoiceId] : ['/invoices'];
    this.facade.navigateTo(route);
  }
}
