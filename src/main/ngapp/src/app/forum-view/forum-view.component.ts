// /home/cmngu/Workspace/BootForum2/src/main/ngapp/src/app/forum-view/forum-view.component.ts
import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ForumService } from '../_services/forum.service';
import { DiscussionService } from '../_services/discussion.service'; // Import DiscussionService
import { ForumDTO, Page, DiscussionDTO } from '../_data/dtos'; // ForumViewDTO is no longer needed here
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Import FormsModule

import { TableModule } from 'primeng/table';

@Component({
  selector: 'app-forum-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    TableModule,
    FormsModule // Add FormsModule here
  ],
  templateUrl: './forum-view.component.html',
  styleUrls: ['./forum-view.component.css']
})
export class ForumViewComponent implements OnInit, OnDestroy {
  forumId: number | null = null;
  forumDetails: ForumDTO | null = null; // For forum-specific details
  discussionsPage: Page<DiscussionDTO> | null = null; // For paginated discussions

  isLoadingForum = true; // Loading state for forum details
  isLoadingDiscussions = true; // Loading state for discussions list

  forumErrorMessage: string | null = null; // Error message for forum details fetching
  discussionsErrorMessage: string | null = null; // Error message for discussions fetching

  currentPage = 0;
  pageSize = 2;
  // Optional: if you want to control sort from component, otherwise service defaults will be used
  // sortProperty = 'stat.lastComment.commentDate';
  // sortDirection = 'DESC';

  private routeSubscription: Subscription | undefined;
  private forumDetailsSubscription: Subscription | undefined;
  private discussionsSubscription: Subscription | undefined;

  private route = inject(ActivatedRoute);
  private forumService = inject(ForumService);
  private discussionService = inject(DiscussionService); // Inject DiscussionService

  ngOnInit(): void {
    this.routeSubscription = this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        const parsedForumId = +idParam;
        if (!isNaN(parsedForumId)) {
          this.forumId = parsedForumId;
          // Fetch forum details and then discussions
          this.fetchForumDetails(this.forumId);
          this.fetchDiscussions(this.forumId, this.currentPage, this.pageSize);
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
  }

  fetchForumDetails(id: number): void {
    this.isLoadingForum = true;
    this.forumErrorMessage = null;
    this.forumDetailsSubscription = this.forumService.getForumById(id).subscribe({
      next: (response) => {
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
        this.forumErrorMessage = err.message || 'An unexpected error occurred while fetching forum details.';
        console.error('HTTP error fetching forum details:', err);
        this.isLoadingForum = false;
      }
    });
  }

  fetchDiscussions(
    forumId: number,
    page: number,
    size: number
    // sortProperty: string = this.sortProperty, // Uncomment if using component-level sort
    // sortDirection: string = this.sortDirection // Uncomment if using component-level sort
  ): void {
    this.isLoadingDiscussions = true;
    this.discussionsErrorMessage = null;
    this.discussionsSubscription = this.discussionService.listDiscussions(
      forumId,
      page,
      size
      // sortProperty, // Uncomment if using component-level sort
      // sortDirection // Uncomment if using component-level sort
    ).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.discussionsPage = response.data;
          this.currentPage = this.discussionsPage.number; // Update current page from response
        } else {
          this.discussionsErrorMessage = response.message || 'Failed to load discussions.';
          if (response.errors) {
            console.error('Error details (discussions):', response.errors);
          }
        }
        this.isLoadingDiscussions = false;
      },
      error: (err) => {
        this.discussionsErrorMessage = err.message || 'An unexpected error occurred while fetching discussions.';
        console.error('HTTP error fetching discussions:', err);
        this.isLoadingDiscussions = false;
      }
    });
  }

  goToPage(pageNumber: number): void {
    if (this.forumId === null || pageNumber < 0 || (this.discussionsPage && pageNumber >= this.discussionsPage.totalPages)) {
      return;
    }
    // No need to update this.currentPage here, fetchDiscussions will update it from response
    this.fetchDiscussions(this.forumId, pageNumber, this.pageSize);
  }

  onPageSizeChange(event: Event): void {
    const newSize = +(event.target as HTMLSelectElement).value;
    this.pageSize = newSize;
    this.currentPage = 0; // Reset to first page
    if (this.forumId !== null) {
      this.fetchDiscussions(this.forumId, this.currentPage, this.pageSize);
    }
  }

  getPageNumbers(): number[] {
    if (!this.discussionsPage || this.discussionsPage.totalPages <= 1) return [];
    const totalPages = this.discussionsPage.totalPages;
    const currentPage = this.discussionsPage.number; // This is 0-indexed from backend
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
    this.routeSubscription?.unsubscribe();
    this.forumDetailsSubscription?.unsubscribe();
    this.discussionsSubscription?.unsubscribe();
  }
}
