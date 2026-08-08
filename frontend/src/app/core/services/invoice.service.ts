import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Invoice, InvoiceStatus, PageResponse } from '../models/invoice.model';
import { environment } from '../../../environments/environment';

export interface InvoiceFilters {
  search?: string;
  status?: InvoiceStatus;
  dateFrom?: string;
  dateTo?: string;
  minAmount?: number;
  maxAmount?: number;
}

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly API_URL = `${environment.apiBaseUrl}/api/v1/invoices`;

  constructor(private http: HttpClient) {}

  getPage(page: number, size: number, filters?: InvoiceFilters): Observable<PageResponse<Invoice>> {
    let url = `${this.API_URL}?page=${page}&size=${size}`;
    if (filters?.search?.trim())    url += `&search=${encodeURIComponent(filters.search.trim())}`;
    if (filters?.status)            url += `&status=${filters.status}`;
    if (filters?.dateFrom)          url += `&dateFrom=${filters.dateFrom}`;
    if (filters?.dateTo)            url += `&dateTo=${filters.dateTo}`;
    if (filters?.minAmount != null) url += `&minAmount=${filters.minAmount}`;
    if (filters?.maxAmount != null) url += `&maxAmount=${filters.maxAmount}`;
    return this.http.get<PageResponse<Invoice>>(url);
  }

  getById(id: number): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.API_URL}/${id}`);
  }

  create(invoice: Invoice): Observable<Invoice> {
    return this.http.post<Invoice>(this.API_URL, invoice);
  }

  update(id: number, invoice: Invoice): Observable<Invoice> {
    return this.http.put<Invoice>(`${this.API_URL}/${id}`, invoice);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  updateStatus(id: number, status: InvoiceStatus): Observable<Invoice> {
    return this.http.patch<Invoice>(`${this.API_URL}/${id}/status`, status);
  }
}
