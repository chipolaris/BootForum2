import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { DiscussionService } from '../_services/discussion.service';
import { AvatarService } from '../_services/avatar.service';
import { DiscussionSummaryDTO, Page, errorMessageFromApiResponse } from '../_data/dtos';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';
import { PaginatorComponent } from '../shared/paginator/paginator.component';

@Component({
  selector: 'app-discussion-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NgIconComponent, PaginatorComponent],
  providers: [provideIcons(APP_ICONS)],
  templateUrl: './discussion-list.component.html',
  styleUrls: ['./discussion-list.component.css']
})
export class DiscussionListComponent implements OnInit {
  private discussionService = inject(DiscussionService);
  private avatarService = inject(AvatarService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  discussionsPage: Page<DiscussionSummaryDTO> | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  avatarFileIdMap: Map<string, number | null> = new Map();

  // Pagination and Sorting State
  currentPage = 0;
  pageSize = 20;
  sortProperty = 'createDate';
  sortOrder = 'DESC';

  // Options for the UI controls
  sortPropertyOptions = [
    { label: 'Date Started', value: 'createDate' },
    { label: 'Comment Count', value: 'stat.commentCount' },
    { label: 'View Count', value: 'stat.viewCount' }
  ];
  sortOrderOptions = [
    { label: 'Descending', value: 'DESC' },
    { label: 'Ascending', value: 'ASC' }
  ];
  pageSizeOptions = [10, 20, 50];

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.currentPage = params['page'] ? +params['page'] : 0;
      this.pageSize = params['size'] ? +params['size'] : 20;
      this.sortProperty = params['sort'] ? params['sort'].split(',')[0] : 'createDate';
      this.sortOrder = params['sort'] ? params['sort'].split(',')[1] : 'DESC';
      this.loadDiscussions();
    });
  }

  loadDiscussions(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.avatarFileIdMap.clear();

    this.discussionService.listDiscussionSummaries({
      page: this.currentPage,
      size: this.pageSize,
      sortProperty: this.sortProperty,
      sortDirection: this.sortOrder
    }).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.discussionsPage = response.data;
          this.fetchAvatarFileIds();
        } else {
          this.errorMessage = errorMessageFromApiResponse(response) || 'Failed to load discussions.';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message || 'An unexpected error occurred.';
        this.isLoading = false;
      }
    });

    this.updateUrl();
  }

  private fetchAvatarFileIds(): void {
    if (!this.discussionsPage?.content) {
      return;
    }
    const usernames = new Set<string>(this.discussionsPage.content.map(d => d.createBy));

    if (usernames.size === 0) {
      return;
    }

    this.avatarService.getAvatarFileIds(Array.from(usernames)).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.avatarFileIdMap = new Map(Object.entries(response.data));
          this.cdr.detectChanges();
        }
      },
      error: (err) => console.error('Failed to fetch avatar file IDs for discussion list:', err)
    });
  }

  getAvatarUrl(username: string): string {
    if (this.avatarFileIdMap.has(username)) {
      const fileId = this.avatarFileIdMap.get(username);
      if (fileId) {
        return `/api/public/files/${fileId}`;
      }
    }
    return '/assets/images/default-avatar.png';
  }

  onSortChange(): void {
    this.currentPage = 0; // Reset to first page on sort change
    this.loadDiscussions();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadDiscussions();
  }

  private updateUrl(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        page: this.currentPage,
        size: this.pageSize,
        sort: `${this.sortProperty},${this.sortOrder}`
      },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }
}
