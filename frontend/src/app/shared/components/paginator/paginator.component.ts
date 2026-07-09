import {
  Component, Input, Output, EventEmitter,
  ChangeDetectionStrategy, OnChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';

export interface PageChangeEvent {
  page: number;
  size: number;
}

@Component({
  selector: 'app-paginator',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './paginator.component.html',
  styleUrls: ['./paginator.component.scss'],
})
export class PaginatorComponent implements OnChanges {
  @Input() currentPage = 0;
  @Input() totalPages  = 0;
  @Input() pageSize    = 5;
  @Input() pageSizeOptions: number[] = [5, 10, 15];
  @Input() totalElements = 0;

  @Output() pageChange = new EventEmitter<PageChangeEvent>();

  pageWindow: number[] = [];

  ngOnChanges(): void {
    this.pageWindow = this.buildPageWindow();
  }

  private buildPageWindow(): number[] {
    const total = this.totalPages;
    if (total === 0) return [];
    if (total <= 5) return Array.from({ length: total }, (_, i) => i);

    let start = Math.max(0, this.currentPage - 2);
    const end = Math.min(total - 1, start + 4);
    start = Math.max(0, end - 4); // ajuste si end fue clamped al borde derecho

    return Array.from({ length: end - start + 1 }, (_, i) => start + i);
  }

  get isFirst(): boolean { return this.currentPage === 0; }
  get isLast():  boolean { return this.currentPage >= this.totalPages - 1; }

  get rangeStart(): number {
    return this.totalElements === 0 ? 0 : this.currentPage * this.pageSize + 1;
  }
  get rangeEnd(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }

  goTo(page: number): void {
    if (page < 0 || page >= this.totalPages || page === this.currentPage) return;
    this.pageChange.emit({ page, size: this.pageSize });
  }

  onSizeChange(event: Event): void {
    const newSize = Number((event.target as HTMLSelectElement).value);
    this.pageChange.emit({ page: 0, size: newSize });
  }
}
