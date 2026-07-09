import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InvoiceStatus } from '../../../core/models/invoice.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="status-badge status-{{ status.toLowerCase() }}">
      <span class="status-dot"></span>
      {{ label }}
    </span>
  `,
  styles: [`
    :host { display: inline-block; }

    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.3px;
      padding: 3px 10px 3px 8px;
      border-radius: 20px;
      white-space: nowrap;
    }

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    .status-pending {
      background: #FEF3C7;
      color: #92400E;
      .status-dot { background: #D97706; }
    }

    .status-paid {
      background: #D1FAE5;
      color: #064E3B;
      .status-dot { background: #059669; }
    }

    .status-cancelled {
      background: #F3F4F6;
      color: #6B7280;
      .status-dot { background: #9CA3AF; }
    }
  `],
})
export class StatusBadgeComponent {
  @Input() status: InvoiceStatus = 'PENDING';

  get label(): string {
    const labels: Record<InvoiceStatus, string> = {
      PENDING: 'Pendiente',
      PAID: 'Pagada',
      CANCELLED: 'Cancelada',
    };
    return labels[this.status];
  }
}
