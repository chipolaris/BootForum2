    // /home/cmngu/Workspace/BootForum2/src/main/ngapp/src/app/forum-view/forum-view.component.ts
    import { Component, OnInit, OnDestroy, inject } from '@angular/core';
    import { ActivatedRoute, RouterModule } from '@angular/router';
    import { ForumService } from '../_services/forum.service';
    import { DiscussionService } from '../_services/discussion.service';
    import { ForumDTO, Page, DiscussionDTO } from '../_data/dtos';
    import { Subscription, distinctUntilChanged, tap, switchMap } from 'rxjs'; // Added distinctUntilChanged, tap, switchMap
    import { CommonModule } from '@angular/common';
    import { FormsModule } from '@angular/forms';

    import { TableModule } from 'primeng/table';
    import { SortEvent } from 'primeng/api'; // Import SortEvent

    @Component({
      selector: 'app-forum-view',
      standalone: true,
      imports: [
        CommonModule,
        RouterModule,
        TableModule,
        FormsModule
      ],
      templateUrl: './forum-view.component.html',
      styleUrls: ['./forum-view.component.css']
    })
    export class ForumViewComponent implements OnInit, OnDestroy {
      forumId: number | null = null;
      forumDetails: ForumDTO | null = null;
      discussionsPage: Page<DiscussionDTO> | null = null;

      isLoadingForum = true;
      isLoadingDiscussions = true;

      forumErrorMessage: string | null = null;
      discussionsErrorMessage: string | null = null;

      currentPage = 0; // Ensure this is always initialized to a valid number
      pageSize = 2;

      currentSortField: string = 'stat.lastComment.commentDate';
      currentSortOrder: string = 'DESC';
      currentSortOrderNumber: number = -1;

      private subscriptions = new Subscription(); // Use a single Subscription to manage all

      private route = inject(ActivatedRoute);
      private forumService = inject(ForumService);
      private discussionService = inject(DiscussionService);

      ngOnInit(): void {
        console.log('ForumViewComponent: ngOnInit started');

        const routeParamsSub = this.route.paramMap.pipe(
          tap(params => console.log('ForumViewComponent: Route params changed', params.get('id'))),
          distinctUntilChanged((prev, curr) => prev.get('id') === curr.get('id')) // Only proceed if 'id' actually changed
        ).subscribe(params => {
          const idParam = params.get('id');
          console.log('ForumViewComponent: Processing route param id:', idParam);

          if (idParam) {
            const parsedForumId = +idParam;
            if (!isNaN(parsedForumId)) {
              if (this.forumId !== parsedForumId) { // Additional check to prevent re-fetch if id hasn't effectively changed
                console.log(`ForumViewComponent: Forum ID changed from ${this.forumId} to ${parsedForumId}. Fetching data.`);
                this.forumId = parsedForumId;
                this.currentPage = 0; // Reset page on new forum ID
                this.fetchForumDetails(this.forumId);
                this.fetchDiscussions(this.forumId, this.currentPage, this.pageSize, this.currentSortField, this.currentSortOrder);
              } else {
                console.log(`ForumViewComponent: Forum ID (${parsedForumId}) is the same as current. Skipping full re-fetch unless necessary.`);
              }
            } else {
              this.isLoadingForum = false;
              this.isLoadingDiscussions = false;
              this.forumErrorMessage = 'Invalid Forum ID in route parameters.';
              console.error(this.forumErrorMessage);
            }
          } else {
            this.isLoadingForum = false;
            this.isLoadingDiscussions = false;
            this.forumErrorMessage = 'Forum ID not found in route parameters.';
            console.error(this.forumErrorMessage);
          }
        });
        this.subscriptions.add(routeParamsSub);
      }

      fetchForumDetails(id: number): void {
        console.log(`ForumViewComponent: fetchForumDetails called for id: ${id}`);
        this.isLoadingForum = true;
        this.forumErrorMessage = null;

        const forumDetailsSub = this.forumService.getForumById(id).subscribe({
          next: (response) => {
            console.log(`ForumViewComponent: fetchForumDetails response for id: ${id}`, response);
            if (response.success && response.data) {
              this.forumDetails = response.data;
            } else {
              this.forumErrorMessage = response.message || 'Failed to load forum details.';
              if (response.errors) {
                console.error('Error details (forum):', response.errors);
              }
            }
            this.isLoadingForum = false;
          },
          error: (err) => {
            console.error(`ForumViewComponent: fetchForumDetails error for id: ${id}`, err);
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
        // Ensure 'page' is a valid number before making the call
        const pageToFetch = (typeof page === 'number' && !isNaN(page)) ? page : 0;
        console.log(`ForumViewComponent: fetchDiscussions called for forumId: ${forumId}, page: ${pageToFetch} (original: ${page}), size: ${size}, sort: ${sortProperty},${sortDirection}`);

        this.isLoadingDiscussions = true;
        this.discussionsErrorMessage = null;

        const discussionsSub = this.discussionService.listDiscussions(
          forumId,
          pageToFetch, // Use the validated pageToFetch
          size,
          sortProperty,
          sortDirection
        ).subscribe({
          next: (response) => {
            console.log(`ForumViewComponent: fetchDiscussions response for forumId: ${forumId}`, response);
            if (response.success && response.data) {
              this.discussionsPage = response.data;
              // Robustly update currentPage
              const newPageNumber = this.discussionsPage.number;
              if (typeof newPageNumber === 'number' && !isNaN(newPageNumber)) {
                if (this.currentPage !== newPageNumber) {
                  console.log(`ForumViewComponent: Updating currentPage from ${this.currentPage} to ${newPageNumber}`);
                  this.currentPage = newPageNumber;
                }
              } else {
                console.warn(`ForumViewComponent: Received invalid page number from backend: ${newPageNumber}. Keeping current page: ${this.currentPage}`);
                // Optionally, reset to 0 if backend provides invalid page number
                // this.currentPage = 0;
              }
            } else {
              this.discussionsErrorMessage = response.message || 'Failed to load discussions.';
              if (response.errors) {
                console.error('Error details (discussions):', response.errors);
              }
            }
            this.isLoadingDiscussions = false;
          },
          error: (err) => {
            console.error(`ForumViewComponent: fetchDiscussions error for forumId: ${forumId}`, err);
            this.discussionsErrorMessage = err.message || 'An unexpected error occurred while fetching discussions.';
            this.isLoadingDiscussions = false;
          }
        });
        this.subscriptions.add(discussionsSub);
      }

      onSortChange(event: SortEvent): void {
        console.log('ForumViewComponent: onSortChange triggered', event);
        if (!this.forumId) {
          console.warn('ForumViewComponent: onSortChange called but forumId is null.');
          return;
        }

        if (event.field && (event.order === 1 || event.order === -1)) {
          const newSortField = event.field;
          const newSortOrderNumber = event.order;
          const newSortOrder = newSortOrderNumber === 1 ? 'ASC' : 'DESC';

          if (this.currentSortField !== newSortField || this.currentSortOrder !== newSortOrder) {
            console.log(`ForumViewComponent: Sort changed from ${this.currentSortField}/${this.currentSortOrder} to ${newSortField}/${newSortOrder}. Fetching discussions.`);
            this.currentSortField = newSortField;
            this.currentSortOrderNumber = newSortOrderNumber;
            this.currentSortOrder = newSortOrder;
            this.currentPage = 0; // Reset to a known valid number
            this.fetchDiscussions(this.forumId, this.currentPage, this.pageSize, this.currentSortField, this.currentSortOrder);
          } else {
            console.warn('ForumViewComponent: onSortChange called, but sort parameters are identical. Suppressing re-fetch.');
          }
        } else {
          console.warn('ForumViewComponent: onSortChange called with incomplete or no-change event data:', event);
        }
      }

      goToPage(pageNumber: number): void {
        console.log(`ForumViewComponent: goToPage called for pageNumber: ${pageNumber}`);
        if (this.forumId === null || pageNumber < 0 || (this.discussionsPage && pageNumber >= this.discussionsPage.totalPages)) {
          return;
        }
        // pageNumber from here should be valid as it's from getPageNumbers() or +/- 1
        this.fetchDiscussions(this.forumId, pageNumber, this.pageSize, this.currentSortField, this.currentSortOrder);
      }

      onPageSizeChange(newSizeValue: number): void {
        const newSize = +newSizeValue;
        console.log(`ForumViewComponent: onPageSizeChange called, newSize: ${newSize}`);
        this.pageSize = newSize;
        this.currentPage = 0; // Reset to a known valid number
        if (this.forumId !== null) {
          this.fetchDiscussions(this.forumId, this.currentPage, this.pageSize, this.currentSortField, this.currentSortOrder);
        }
      }

      getPageNumbers(): number[] {
        if (
          !this.discussionsPage ||
          typeof this.discussionsPage.totalPages !== 'number' ||
          isNaN(this.discussionsPage.totalPages) ||
          this.discussionsPage.totalPages <= 0 || // Changed from <=1 to <=0 if a single page shouldn't show numbers
          typeof this.discussionsPage.number !== 'number' ||
          isNaN(this.discussionsPage.number)
        ) {
          // If totalPages is 1, we might still want to hide pagination numbers,
          // so adjust totalPages <= 0 or totalPages < 2 as per your UI preference for a single page.
          // For now, if totalPages is 1, it will also return [], hiding numbered buttons.
          if (this.discussionsPage && typeof this.discussionsPage.totalPages === 'number' && this.discussionsPage.totalPages === 1) {
            return []; // Explicitly return empty for a single page if no numbers needed
          }
// Modify the console.warn line in getPageNumbers:
console.warn(
  'getPageNumbers: discussionsPage or its critical properties (totalPages, number) are invalid or indicate no pagination needed. Actual state at time of warning:',
  JSON.stringify(this.discussionsPage || null) // Log a snapshot
);          return [];
        }

        const totalPages = this.discussionsPage.totalPages;
        const currentPage = this.discussionsPage.number; // This is 0-indexed and validated above
        const maxPagesToShow = 5;
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
            if (startPage > 1) pages.push(-1); // Ellipsis
          }
          for (let i = startPage; i <= endPage; i++) pages.push(i);
          if (endPage < totalPages - 1) {
            if (endPage < totalPages - 2) pages.push(-1); // Ellipsis
            pages.push(totalPages - 1);
          }
        }
        return pages;
      }


      ngOnDestroy(): void {
        console.log('ForumViewComponent: ngOnDestroy called, unsubscribing.');
        this.subscriptions.unsubscribe();
      }
    }
