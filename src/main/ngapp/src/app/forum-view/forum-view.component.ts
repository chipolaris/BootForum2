import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ForumService } from '../_services/forum.service';
import { DiscussionService } from '../_services/discussion.service';
import { ForumDTO, Page, DiscussionSummaryDTO } from '../_data/dtos';
import { Subscription, distinctUntilChanged, tap, switchMap } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { SortEvent } from 'primeng/api';

import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';
import { PaginatorComponent } from '../shared/paginator/paginator.component'; // <-- IMPORT PAGINATOR

@Component({
  selector: 'app-forum-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TableModule,
    FormsModule,
    NgIconComponent,
    PaginatorComponent // <-- ADD PAGINATOR TO IMPORTS
  ],
  providers: [provideIcons(APP_ICONS)],
  templateUrl: './forum-view.component.html',
  styleUrls: ['./forum-view.component.css']
})
export class ForumViewComponent implements OnInit, OnDestroy {
  forumId: number | null = null;
  forumDetails: ForumDTO | null = null;
  discussionsPage: Page<DiscussionSummaryDTO> | null = null;

  isLoadingForum = true;
  isLoadingDiscussions = true;

  forumErrorMessage: string | null = null;
  discussionsErrorMessage: string | null = null;

  currentPage = 0;
  pageSize = 10;

  currentSortField: string = 'stat.lastComment.commentDate';
  currentSortOrder: string = 'DESC';
  currentSortOrderNumber: number = -1;

  // REMOVED: displayablePageNumbers is no longer needed

  private subscriptions = new Subscription();

  private route = inject(ActivatedRoute);
  private forumService = inject(ForumService);
  private discussionService = inject(DiscussionService);

  ngOnInit(): void {
    const routeParamsSub = this.route.paramMap.pipe(
      distinctUntilChanged((prev, curr) => prev.get('id') === curr.get('id'))
    ).subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        const parsedForumId = +idParam;
        if (!isNaN(parsedForumId)) {
          if (this.forumId !== parsedForumId) {
            this.forumId = parsedForumId;
            this.currentPage = 0; // Reset page on new forum ID
            this.fetchForumDetails(this.forumId);
            this.fetchDiscussions(this.forumId, this.currentPage, this.pageSize, this.currentSortField, this.currentSortOrder);
          }
        } else {
          this.isLoadingForum = false;
          this.isLoadingDiscussions = false;
          this.forumErrorMessage = 'Invalid Forum ID in route parameters.';
        }
      } else {
        this.isLoadingForum = false;
        this.isLoadingDiscussions = false;
        this.forumErrorMessage = 'Forum ID not found in route parameters.';
      }
    });
    this.subscriptions.add(routeParamsSub);
  }

  fetchForumDetails(id: number): void {
    this.isLoadingForum = true;
    this.forumErrorMessage = null;

    const forumDetailsSub = this.forumService.getForumById(id).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.forumDetails = response.data;
        } else {
          this.forumErrorMessage = response.message || 'Failed to load forum details.';
        }
        this.isLoadingForum = false;
      },
      error: (err) => {
        this.forumErrorMessage = err.message || 'An unexpected error occurred while fetching forum details.';
        this.isLoadingForum = false;
      }
    });
    this.subscriptions.add(forumDetailsSub);
  }

  fetchDiscussions(
    forumId: number,
    page: number,
    size: number,
    sortProperty: string,
    sortDirection: string
  ): void {
    const pageToFetch = (typeof page === 'number' && !isNaN(page)) ? page : 0;
    this.isLoadingDiscussions = true;
    this.discussionsErrorMessage = null;

    const discussionsSub = this.discussionService.listDiscussionSummariesByForum(
      forumId,
      pageToFetch,
      size,
      sortProperty,
      sortDirection
    ).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.discussionsPage = response.data;
          this.currentPage = this.discussionsPage.number;
          // REMOVED: No longer need to calculate displayable page numbers
        } else {
          this.discussionsErrorMessage = response.message || 'Failed to load discussions.';
        }
        this.isLoadingDiscussions = false;
      },
      error: (err) => {
        this.discussionsErrorMessage = err.message || 'An unexpected error occurred while fetching discussions.';
        this.isLoadingDiscussions = false;
      }
    });
    this.subscriptions.add(discussionsSub);
  }

  onSortChange(event: SortEvent): void {
    if (!this.forumId) return;

    if (event.field && (event.order === 1 || event.order === -1)) {
      const newSortField = event.field;
      const newSortOrderNumber = event.order;
      const newSortOrder = newSortOrderNumber === 1 ? 'ASC' : 'DESC';

      if (this.currentSortField !== newSortField || this.currentSortOrder !== newSortOrder) {
        this.currentSortField = newSortField;
        this.currentSortOrderNumber = newSortOrderNumber;
        this.currentSortOrder = newSortOrder;
        this.currentPage = 0;
        this.fetchDiscussions(this.forumId, this.currentPage, this.pageSize, this.currentSortField, this.currentSortOrder);
      }
    }
  }

  // RENAMED from goToPage for consistency
  onPageChange(pageNumber: number): void {
    if (this.forumId === null || pageNumber < 0 || (this.discussionsPage && pageNumber >= this.discussionsPage.totalPages)) {
      return;
    }
    this.fetchDiscussions(this.forumId, pageNumber, this.pageSize, this.currentSortField, this.currentSortOrder);
  }

  onPageSizeChange(newSizeValue: string | number): void {
    const newSize = +newSizeValue;
    if (this.pageSize === newSize) {
      return;
    }
    this.pageSize = newSize;
    this.currentPage = 0;
    if (this.forumId !== null) {
      this.fetchDiscussions(this.forumId, this.currentPage, this.pageSize, this.currentSortField, this.currentSortOrder);
    }
  }

  // REMOVED: _calculateDisplayablePageNumbers method is no longer needed.

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }
}
