export type InvoiceStatus = 'PENDING' | 'PAID' | 'CANCELLED';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface InvoiceItem {
  id?: number;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal?: number;
  sortOrder?: number;
}

export interface Invoice {
  id?: number;
  invoiceNumber: string;
  issueDate: string;
  status: InvoiceStatus;
  clientName: string;
  clientEmail: string;
  clientAddress: string;
  subtotal?: number;
  taxRate: number;
  taxAmount?: number;
  total?: number;
  notes?: string;
  items: InvoiceItem[];
  createdAt?: string;
  updatedAt?: string;
}
