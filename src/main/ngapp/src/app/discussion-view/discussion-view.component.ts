import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  ChangeDetectorRef,
} from '@angular/core';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, distinctUntilChanged, tap, of, forkJoin } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MessageService } from 'primeng/api';

import { DiscussionService } from '../_services/discussion.service';
import { CommentService } from '../_services/comment.service';
import { VoteService } from '../_services/vote.service';
import { AuthenticationService } from '../_services/authentication.service';
import { AvatarService } from '../_services/avatar.service';
import { DiscussionDTO, CommentDTO, FileInfoDTO, Page, ApiResponse, DiscussionSummaryDTO } from '../_data/dtos';
import { FileListComponent } from '../file-list/file-list.component';

import { NgIcon, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';

import { MarkdownModule } from 'ngx-markdown';

// PrimeNG Modules for Galleria
import { GalleriaModule } from 'primeng/galleria';
import { DialogModule } from 'primeng/dialog';
import { PaginatorComponent } from '../shared/paginator/paginator.component';

@Component({
  selector: 'app-discussion-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    NgIcon,
    DatePipe,
    MarkdownModule,
    FileListComponent,
    GalleriaModule,
    DialogModule,
    PaginatorComponent
  ],
  providers: [
    provideIcons(APP_ICONS)
  ],
  templateUrl: './discussion-view.component.html',
  styleUrls: ['./discussion-view.component.css']
})
export class DiscussionViewComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private discussionService = inject(DiscussionService);
  private commentService = inject(CommentService);
  private voteService = inject(VoteService);
  private authService = inject(AuthenticationService);
  private avatarService = inject(AvatarService);
  private messageService = inject(MessageService);
  private cdr = inject(ChangeDetectorRef);

  discussionId: number | null = null;
  discussionDetails: DiscussionDTO | null = null;

  commentsPage: Page<CommentDTO> | null = null;
  isLoadingComments = true;
  commentsError: string | null = null;
  currentCommentsPage = 0;
  commentsPageSize = 10;
  commentsSortField: string = 'createDate';
  commentsSortDirection: string = 'ASC';
  commentSortOption: string = 'createDate,ASC'; // New property for the dropdown
  commentsDataForViewers: CommentDTO[] = [];

  isLoadingDiscussion = true;
  discussionError: string | null = null;

  // NEW: State for similar discussions
  similarDiscussions: DiscussionSummaryDTO[] = [];
  isLoadingSimilar = false;
  similarDiscussionsLoaded = false;
  similarDiscussionsError: string | null = null;

  avatarFileIdMap: Map<string, number | null> = new Map();

  private subscriptions = new Subscription();

  galleriaVisible: boolean = false;
  currentGalleriaImages: any[] = [];

  responsiveOptions: any[] = [
    { breakpoint: '1024px', numVisible: 5 },
    { breakpoint: '768px', numVisible: 3 },
    { breakpoint: '560px', numVisible: 1 }
  ];

  ngOnInit(): void {
    const routeParamsSub = this.route.paramMap.pipe(
      distinctUntilChanged((prev, curr) => prev.get('id') === curr.get('id'))
    ).subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        const parsedDiscussionId = +idParam;
        if (!isNaN(parsedDiscussionId)) {
          if (this.discussionId !== parsedDiscussionId) {
            this.discussionId = parsedDiscussionId;
            this.resetDataStates();
            this.loadDiscussionData(this.discussionId);
          }
        } else {
          this.handleError('Invalid Discussion ID in route.', 'discussion');
        }
      } else {
        this.handleError('Discussion ID not found in route.', 'discussion');
      }
    });
    this.subscriptions.add(routeParamsSub);
  }

  private resetDataStates(): void {
    this.discussionDetails = null;
    this.commentsPage = null;
    this.commentsDataForViewers = [];
    this.avatarFileIdMap.clear();
    this.currentCommentsPage = 0;
    this.isLoadingDiscussion = true;
    this.discussionError = null;
    this.isLoadingComments = true;
    this.commentsError = null;
    // NEW: Reset similar discussions state
    this.similarDiscussions = [];
    this.isLoadingSimilar = false;
    this.similarDiscussionsLoaded = false;
    this.similarDiscussionsError = null;
  }

  private loadDiscussionData(discussionId: number): void {
    this.isLoadingDiscussion = true;
    this.isLoadingComments = true;

    const discussion$ = this.discussionService.getDiscussionById(discussionId);
    const comments$ = this.commentService.listComments(discussionId, 0, this.commentsPageSize, this.commentsSortField, this.commentsSortDirection);

    const loadSub = forkJoin([discussion$, comments$]).subscribe({
      next: ([discussionResponse, commentsResponse]) => {
        if (discussionResponse.success && discussionResponse.data) {
          this.discussionDetails = discussionResponse.data;
        } else {
          this.handleError(discussionResponse.message || 'Failed to load discussion details.', 'discussion', discussionResponse.errors);
        }
        this.isLoadingDiscussion = false;

        if (commentsResponse?.success && commentsResponse.data) {
          this.commentsPage = commentsResponse.data;
          this.commentsDataForViewers = this.commentsPage.content;
          this.currentCommentsPage = this.commentsPage.number;
        } else if (commentsResponse && !commentsResponse.success) {
          this.handleError(commentsResponse.message || 'Failed to load comments.', 'comments', commentsResponse.errors);
        }
        this.isLoadingComments = false;

        this.fetchAvatarFileIds();
      },
      error: (err) => {
        this.handleError(err.message || 'Error loading discussion or comments.', 'discussion');
        this.isLoadingDiscussion = false;
        this.isLoadingComments = false;
      }
    });
    this.subscriptions.add(loadSub);
  }

  /**
   * NEW: Fetches similar discussions on demand.
   */
  loadSimilarDiscussions(): void {
    if (!this.discussionId || this.isLoadingSimilar) {
      return;
    }
    this.isLoadingSimilar = true;
    this.similarDiscussionsLoaded = true; // Mark as loaded to show the section
    this.similarDiscussionsError = null;

    const sub = this.discussionService.getSimilarDiscussions(this.discussionId).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.similarDiscussions = response.data;
        } else {
          this.similarDiscussionsError = response.message || 'Could not load similar discussions.';
        }
        this.isLoadingSimilar = false;
      },
      error: (err) => {
        this.similarDiscussionsError = err.message || 'An unexpected error occurred.';
        this.isLoadingSimilar = false;
      }
    });
    this.subscriptions.add(sub);
  }

  fetchComments(discussionId: number, pageForComments: number, size: number, sortProperty: string, sortDirection: string): void {
    this.isLoadingComments = true;
    this.commentsError = null;

    const sub = this.commentService.listComments(discussionId, pageForComments, size, sortProperty, sortDirection)
      .pipe(catchError(err => {
        this.handleError(err.message || 'Error fetching comments.', 'comments');
        return of(null);
      }))
      .subscribe(response => {
        if (response?.success && response.data) {
          this.commentsPage = response.data;
          this.commentsDataForViewers = this.commentsPage.content;
          this.currentCommentsPage = this.commentsPage.number;
          this.fetchAvatarFileIds();
        } else if (response && !response.success) {
          this.handleError(response.message || 'Failed to load comments.', 'comments', response.errors);
        }
        this.isLoadingComments = false;
        this.cdr.detectChanges();
      });
    this.subscriptions.add(sub);
  }

  private fetchAvatarFileIds(): void {
    const usernames = new Set<string>();
    if (this.discussionDetails) {
      usernames.add(this.discussionDetails.createBy);
    }
    this.commentsDataForViewers.forEach(comment => usernames.add(comment.createBy));

    if (usernames.size === 0) {
      return;
    }

    const avatarSub = this.avatarService.getAvatarFileIds(Array.from(usernames)).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.avatarFileIdMap = new Map(Object.entries(response.data));
          this.cdr.detectChanges();
        }
      },
      error: (err) => console.error('Failed to fetch avatar file IDs:', err)
    });
    this.subscriptions.add(avatarSub);
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

  voteForDiscussion(voteValue: 'up' | 'down'): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/app/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (!this.discussionDetails?.id) return;
    this.voteService.voteOnDiscussion(this.discussionDetails.id, voteValue).subscribe({
      next: (response) => {
        if (response.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Your vote has been recorded!' });
          if (this.discussionDetails?.stat) {
            if (voteValue === 'up') this.discussionDetails.stat.voteUpCount = (this.discussionDetails.stat.voteUpCount ?? 0) + 1;
            else this.discussionDetails.stat.voteDownCount = (this.discussionDetails.stat.voteDownCount ?? 0) + 1;
          }
        } else {
          const errorMessage = response.errors?.join(', ') || response.message || 'Failed to record vote.';
          this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessage });
        }
      },
      error: (err) => {
        console.error('Error voting on discussion:', err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'An unexpected error occurred.' });
      }
    });
  }

  voteForComment(comment: CommentDTO, voteValue: 'up' | 'down'): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/app/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (!comment.id) return;
    this.voteService.voteOnComment(comment.id, voteValue).subscribe({
      next: (response) => {
        if (response.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Your vote has been recorded!' });
          if (!comment.commentVote) comment.commentVote = { voteUpCount: 0, voteDownCount: 0 };
          if (voteValue === 'up') comment.commentVote.voteUpCount = (comment.commentVote.voteUpCount ?? 0) + 1;
          else comment.commentVote.voteDownCount = (comment.commentVote.voteDownCount ?? 0) + 1;
        } else {
          const errorMessage = response.errors?.join(', ') || response.message || 'Failed to record vote.';
          this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessage });
        }
      },
      error: (err) => {
        console.error('Error voting on comment:', err);
        this.messageService.add({ severity: 'error', summary: 'Error', detail: 'An unexpected error occurred.' });
      }
    });
  }

  private handleError(message: string, type: 'discussion' | 'comments', errors?: string[] | null): void {
    const fullMessage = errors && errors.length > 0 ? `${message} Details: ${errors.join(', ')}` : message;
    if (type === 'discussion') {
      this.discussionError = fullMessage;
      this.isLoadingDiscussion = false;
    } else {
      this.commentsError = fullMessage;
      this.isLoadingComments = false;
    }
    console.error(`Error (${type}): ${fullMessage}`);
  }

  onCommentsPageChange(pageNumber: number): void {
    if (this.discussionId === null || pageNumber < 0 || (this.commentsPage && pageNumber >= this.commentsPage.totalPages)) {
      return;
    }
    this.fetchComments(this.discussionId, pageNumber, this.commentsPageSize, this.commentsSortField, this.commentsSortDirection);
  }

  onCommentsPageSizeChange(newSizeValue: string | number): void {
    const newSize = +newSizeValue;
    if (this.commentsPageSize === newSize) {
        return;
    }
    this.commentsPageSize = newSize;
    this.currentCommentsPage = 0;
    if (this.discussionId !== null) {
      this.fetchComments(this.discussionId, this.currentCommentsPage, this.commentsPageSize, this.commentsSortField, this.commentsSortDirection);
    }
  }

  /**
   * New method to handle sort changes from the dropdown.
   */
  onCommentSortOptionChange(): void {
    if (!this.discussionId) return;

    const [field, direction] = this.commentSortOption.split(',');
    this.commentsSortField = field;
    this.commentsSortDirection = direction;
    this.currentCommentsPage = 0; // Reset to first page on sort change
    this.fetchComments(this.discussionId, this.currentCommentsPage, this.commentsPageSize, this.commentsSortField, this.commentsSortDirection);
  }

  // REMOVED unused onCommentsSortChange method

  public getObjectKeys(obj: any): string[] {
    return obj ? Object.keys(obj) : [];
  }

  public trackByCommentId(index: number, comment: CommentDTO): number | undefined {
    return comment.id;
  }

  ngOnDestroy(): void {
    console.log('[ngOnDestroy] Called. Unsubscribing.');
    this.subscriptions.unsubscribe();
  }

  // --- Galleria Methods ---
  private formatImagesForGalleria(files: FileInfoDTO[] | null | undefined): any[] {
    if (!files || files.length === 0) return [];
    return files.map(file => ({
      image: `/api/public/files/${file.id}`,
      thumbnail: `/api/public/files/${file.id}`,
      alt: file.originalFilename,
      title: file.originalFilename
    }));
  }

  openDiscussionGalleria(): void {
    if (this.discussionDetails?.images && this.discussionDetails.images.length > 0) {
      this.currentGalleriaImages = this.formatImagesForGalleria(this.discussionDetails.images);
      this.galleriaVisible = true;
    }
  }

  openCommentGalleria(comment: CommentDTO): void {
    if (comment.images && comment.images.length > 0) {
      this.currentGalleriaImages = this.formatImagesForGalleria(comment.images);
      this.galleriaVisible = true;
    }
  }

  closeGalleria(): void {
    this.galleriaVisible = false;
    this.currentGalleriaImages = [];
  }
}
