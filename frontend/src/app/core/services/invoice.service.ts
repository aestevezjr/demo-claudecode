import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Invoice, InvoiceStatus, PageResponse } from '../models/invoice.model';

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly API_URL = 'http://localhost:8080/api/v1/invoices';

  constructor(private http: HttpClient) {}

  getPage(page: number, size: number, search?: string): Observable<PageResponse<Invoice>> {
    let url = `${this.API_URL}?page=${page}&size=${size}`;
    if (search?.trim()) url += `&search=${encodeURIComponent(search.trim())}`;
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
