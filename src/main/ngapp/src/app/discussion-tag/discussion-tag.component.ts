import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { DiscussionService } from '../_services/discussion.service';
import { AvatarService } from '../_services/avatar.service';
import { TagService } from '../_services/tag.service';
import { DiscussionSummaryDTO, Page, TagDTO, errorMessageFromApiResponse } from '../_data/dtos';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { PaginatorComponent } from '../shared/paginator/paginator.component';
import { MultiSelectModule } from 'primeng/multiselect';

@Component({
  selector: 'app-discussion-tag',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, NgIconComponent, PaginatorComponent, MultiSelectModule],
  templateUrl: './discussion-tag.component.html',
  styleUrls: ['./discussion-tag.component.css']
})
export class DiscussionTagComponent implements OnInit {
  private discussionService = inject(DiscussionService);
  private avatarService = inject(AvatarService);
  private tagService = inject(TagService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  discussionsPage: Page<DiscussionSummaryDTO> | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  availableTags: TagDTO[] = [];
  selectedTagIds: number[] = [];

  avatarFileIdMap: Map<string, number | null> = new Map();

  // Pagination State
  currentPage = 0;
  pageSize = 25;
  pageSizeOptions = [10, 25, 50];

  ngOnInit(): void {
    this.loadAvailableTags();

    this.route.queryParams.subscribe(params => {
      // Safely parse tagIds from URL which can be a single value or an array
      const tagIdsParam = params['tagIds'];
      if (tagIdsParam) {
        this.selectedTagIds = (Array.isArray(tagIdsParam) ? tagIdsParam : [tagIdsParam]).map(id => +id);
      } else {
        this.selectedTagIds = [];
      }

      this.currentPage = params['page'] ? +params['page'] : 0;
      this.pageSize = params['size'] ? +params['size'] : 25;

      this.loadDiscussions();
    });
  }

  loadAvailableTags(): void {
    this.tagService.getAllTags().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.availableTags = response.data.filter(tag => !tag.disabled);
        }
      },
      error: (err) => console.error('Failed to load available tags', err)
    });
  }

  loadDiscussions(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.avatarFileIdMap.clear();

    // Only fetch discussions if at least one tag is selected
    if (this.selectedTagIds.length === 0) {
      this.discussionsPage = null;
      this.isLoading = false;
      this.updateUrl();
      return;
    }

    this.discussionService.listDiscussionsByTags({
      tagIds: this.selectedTagIds,
      page: this.currentPage,
      size: this.pageSize
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
    if (!this.discussionsPage?.content) return;
    const usernames = new Set<string>(this.discussionsPage.content.map(d => d.createBy));
    if (usernames.size === 0) return;

    this.avatarService.getAvatarFileIds(Array.from(usernames)).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.avatarFileIdMap = new Map(Object.entries(response.data));
          this.cdr.detectChanges();
        }
      }
    });
  }

  getAvatarUrl(username: string): string {
    const fileId = this.avatarFileIdMap.get(username);
    return fileId ? `/api/public/files/${fileId}` : '/assets/images/default-avatar.png';
  }

  onSelectionChange(): void {
    this.currentPage = 0; // Reset to first page on any filter change
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
        tagIds: this.selectedTagIds.length > 0 ? this.selectedTagIds : null,
        page: this.currentPage,
        size: this.pageSize
      },
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }
}
