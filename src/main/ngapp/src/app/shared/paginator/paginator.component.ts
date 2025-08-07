import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-paginator',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './paginator.component.html',
  styleUrls: ['./paginator.component.css']
})
export class PaginatorComponent implements OnChanges {
  @Input() currentPage: number = 0;
  @Input() totalPages: number = 0;
  @Output() pageChange = new EventEmitter<number>();

  displayablePageNumbers: number[] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['currentPage'] || changes['totalPages']) {
      this.calculatePageNumbers();
    }
  }

  private calculatePageNumbers(): void {
    const pageNumbers: number[] = [];
    const pageRange = 2; // How many pages to show around the current page
    const ellipsis = -1; // Sentinel value for ellipsis

    // Always add the first page
    pageNumbers.push(0);

    // Add ellipsis if needed after the first page
    if (this.currentPage > pageRange + 1) {
      pageNumbers.push(ellipsis);
    }

    // Add pages around the current page
    const startPage = Math.max(1, this.currentPage - pageRange);
    const endPage = Math.min(this.totalPages - 2, this.currentPage + pageRange);

    for (let i = startPage; i <= endPage; i++) {
      pageNumbers.push(i);
    }

    // Add ellipsis if needed before the last page
    if (this.currentPage < this.totalPages - pageRange - 2) {
      pageNumbers.push(ellipsis);
    }

    // Always add the last page if there's more than one page
    if (this.totalPages > 1) {
      pageNumbers.push(this.totalPages - 1);
    }

    this.displayablePageNumbers = [...new Set(pageNumbers)]; // Remove duplicates
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }
}
