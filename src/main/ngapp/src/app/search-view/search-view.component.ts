import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DiscussionService } from '../_services/discussion.service';
import { CommentService } from '../_services/comment.service';
import { Page, DiscussionInfoDTO, CommentDTO } from '../_data/dtos';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { HighlightPipe } from '../_pipes/highlight.pipe';

@Component({
  selector: 'app-search-view',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NgIcon, HighlightPipe], // Add HighlightPipe to imports
  templateUrl: './search-view.component.html',
  styleUrls: ['./search-view.component.css']
})
export class SearchViewComponent {
  private discussionService = inject(DiscussionService);
  private commentService = inject(CommentService); // <-- INJECT
  private cdr = inject(ChangeDetectorRef);

  // Search form state
  keyword: string = '';
  searchType: 'discussion' | 'comment' = 'discussion';

  // Results state
  isLoading = false;
  error: string | null = null;
  discussionResultsPage: Page<DiscussionInfoDTO> | null = null;
  commentResultsPage: Page<CommentDTO> | null = null;
  displayablePageNumbers: number[] = [];

  // To track if a search has been performed, so we can show "No results" message correctly
  searchPerformed = false;

  constructor() {}

  /**
   * Returns the currently active results page based on the searchType.
   * This simplifies the template logic for pagination and result display.
   */
  get activeResultsPage(): Page<any> | null {
    return this.searchType === 'discussion' ? this.discussionResultsPage : this.commentResultsPage;
  }

  performSearch(page: number = 0): void {
    if (!this.keyword.trim()) {
      this.error = 'Please enter a keyword to search.';
      return;
    }

    this.isLoading = true;
    this.error = null;
    this.discussionResultsPage = null; // Reset both
    this.commentResultsPage = null;  // Reset both
    this.searchPerformed = true;

    if (this.searchType === 'discussion') {
      this.discussionService.searchDiscussions(this.keyword, page).subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.discussionResultsPage = response.data;
            this.displayablePageNumbers = this._calculateDisplayablePageNumbers(this.discussionResultsPage);
          } else {
            this.error = response.message || 'Failed to fetch discussion search results.';
          }
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error = err.message || 'An unexpected error occurred during the search.';
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    } else if (this.searchType === 'comment') {
      this.commentService.searchComments(this.keyword, page).subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.commentResultsPage = response.data;
            this.displayablePageNumbers = this._calculateDisplayablePageNumbers(this.commentResultsPage);
          } else {
            this.error = response.message || 'Failed to fetch comment search results.';
          }
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error = err.message || 'An unexpected error occurred during the search.';
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  goToPage(pageNumber: number): void {
    if (this.activeResultsPage === null || pageNumber < 0 || pageNumber >= this.activeResultsPage.totalPages) {
      return;
    }
    this.performSearch(pageNumber);
  }

  // Reusable pagination logic - no changes needed here
  private _calculateDisplayablePageNumbers(pageData: Page<any> | null): number[] {
    if (!pageData || typeof pageData.totalPages !== 'number' || pageData.totalPages <= 0) {
      return [];
    }
    const totalPages = pageData.totalPages;
    const currentPage = pageData.number;
    const maxPagesToShow = 10;
    const pages: number[] = [];

    if (totalPages <= maxPagesToShow) {
      for (let i = 0; i < totalPages; i++) pages.push(i);
    } else {
      let startPage = Math.max(0, currentPage - Math.floor(maxPagesToShow / 2));
      let endPage = Math.min(totalPages - 1, startPage + maxPagesToShow - 1);
      if (endPage - startPage + 1 < maxPagesToShow) {
        startPage = Math.max(0, endPage - maxPagesToShow + 1);
      }
      if (startPage > 0) {
        pages.push(0);
        if (startPage > 1) pages.push(-1); // Ellipsis marker
      }
      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }
      if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) pages.push(-1); // Ellipsis marker
        pages.push(totalPages - 1);
      }
    }
    return pages;
  }
}
