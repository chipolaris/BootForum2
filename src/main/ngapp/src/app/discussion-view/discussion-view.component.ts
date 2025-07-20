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
import { Subscription, distinctUntilChanged, tap, of, forkJoin } from 'rxjs'; // Import forkJoin
import { catchError } from 'rxjs/operators';
import { MessageService } from 'primeng/api';

import { DiscussionService } from '../_services/discussion.service';
import { CommentService } from '../_services/comment.service';
import { VoteService } from '../_services/vote.service';
import { AuthenticationService } from '../_services/authentication.service';
import { AvatarService } from '../_services/avatar.service'; // <-- IMPORT NEW SERVICE
import { DiscussionDTO, CommentDTO, FileInfoDTO, Page, ApiResponse } from '../_data/dtos';
import { FileListComponent } from '../file-list/file-list.component'

import { NgIcon, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';

import { MarkdownModule } from 'ngx-markdown';

// PrimeNG Modules for Galleria
import { GalleriaModule } from 'primeng/galleria';
import { DialogModule } from 'primeng/dialog';

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
    DialogModule
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
  private avatarService = inject(AvatarService); // <-- INJECT NEW SERVICE
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
  displayableCommentPageNumbers: number[] = [];
  commentsDataForViewers: CommentDTO[] = [];

  isLoadingDiscussion = true;
  discussionError: string | null = null;

  // --- NEW: Property to store the avatar map ---
  avatarFileIdMap: Map<string, number | null> = new Map();

  private subscriptions = new Subscription();

  // --- Galleria Properties ---
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
    this.avatarFileIdMap.clear(); // Clear the avatar map on reset
    this.currentCommentsPage = 0;
    this.displayableCommentPageNumbers = [];
    this.isLoadingDiscussion = true;
    this.discussionError = null;
    this.isLoadingComments = true;
    this.commentsError = null;
  }

  private loadDiscussionData(discussionId: number): void {
    this.isLoadingDiscussion = true;
    this.isLoadingComments = true;

    // Use forkJoin to wait for both discussion and comments to load before fetching avatars
    const discussion$ = this.discussionService.getDiscussionById(discussionId);
    const comments$ = this.commentService.listComments(discussionId, 0, this.commentsPageSize, this.commentsSortField, this.commentsSortDirection);

    const loadSub = forkJoin([discussion$, comments$]).subscribe({
      next: ([discussionResponse, commentsResponse]) => {
        // Process discussion
        if (discussionResponse.success && discussionResponse.data) {
          this.discussionDetails = discussionResponse.data;
        } else {
          this.handleError(discussionResponse.message || 'Failed to load discussion details.', 'discussion', discussionResponse.errors);
        }
        this.isLoadingDiscussion = false;

        // Process comments
        if (commentsResponse?.success && commentsResponse.data) {
          this.commentsPage = commentsResponse.data;
          this.commentsDataForViewers = this.commentsPage.content;
          this.currentCommentsPage = this.commentsPage.number;
          this.displayableCommentPageNumbers = this._calculateDisplayablePageNumbers(this.commentsPage);
        } else if (commentsResponse && !commentsResponse.success) {
          this.handleError(commentsResponse.message || 'Failed to load comments.', 'comments', commentsResponse.errors);
        }
        this.isLoadingComments = false;

        // After both are loaded, collect usernames and fetch avatar IDs
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

  // This method is now called from within loadDiscussionData after comments are fetched
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
          this.displayableCommentPageNumbers = this._calculateDisplayablePageNumbers(this.commentsPage);
          this.fetchAvatarFileIds(); // Fetch avatars for the new page of comments
        } else if (response && !response.success) {
          this.handleError(response.message || 'Failed to load comments.', 'comments', response.errors);
        }
        this.isLoadingComments = false;
        this.cdr.detectChanges();
      });
    this.subscriptions.add(sub);
  }

  /**
   * Collects all unique usernames and fetches their avatar file IDs.
   */
  private fetchAvatarFileIds(): void {
    const usernames = new Set<string>();
    if (this.discussionDetails) {
      usernames.add(this.discussionDetails.createBy);
    }
    this.commentsDataForViewers.forEach(comment => usernames.add(comment.createBy));

    if (usernames.size === 0) {
      return; // Nothing to fetch
    }

    const avatarSub = this.avatarService.getAvatarFileIds(Array.from(usernames)).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          // The backend returns a plain object, which we convert to a Map
          this.avatarFileIdMap = new Map(Object.entries(response.data));
          this.cdr.detectChanges(); // Manually trigger change detection
        }
      },
      error: (err) => console.error('Failed to fetch avatar file IDs:', err)
    });
    this.subscriptions.add(avatarSub);
  }

  /**
   * Generates the correct avatar URL using the fetched file ID map.
   */
  getAvatarUrl(username: string): string {
    if (this.avatarFileIdMap.has(username)) {
      const fileId = this.avatarFileIdMap.get(username);
      if (fileId) {
        // Use the direct file serving endpoint for a permanent, cacheable URL
        return `/api/public/files/${fileId}`;
      }
    }
    // If user is not in the map or their fileId is null, return the default avatar
    return '/assets/images/default-avatar.png';
  }

  // ... (rest of the component methods: voteForDiscussion, voteForComment, handleError, etc.)
  // ... (No changes needed for the rest of the file)
  voteForDiscussion(voteValue: 'up' | 'down'): void {

    if (!this.authService.isAuthenticated()) {
      // Redirect to login page, preserving the current URL to return to after login
      this.router.navigate(['/app/login'], { queryParams: { returnUrl: this.router.url } });
      return; // Stop further execution
    }

    if (!this.discussionDetails?.id) {
      return;
    }

    this.voteService.voteOnDiscussion(this.discussionDetails.id, voteValue).subscribe({
      next: (response) => {
        if (response.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Your vote has been recorded!' });
          // Optimistically update the UI
          if (this.discussionDetails?.stat) {
            if (voteValue === 'up') {
              this.discussionDetails.stat.voteUpCount = (this.discussionDetails.stat.voteUpCount ?? 0) + 1;
            } else {
              this.discussionDetails.stat.voteDownCount = (this.discussionDetails.stat.voteDownCount ?? 0) + 1;
            }
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
      // Redirect to login page, preserving the current URL to return to after login
      this.router.navigate(['/app/login'], { queryParams: { returnUrl: this.router.url } });
      return; // Stop further execution
    }

    if (!comment.id) {
      return;
    }

    this.voteService.voteOnComment(comment.id, voteValue).subscribe({
      next: (response) => {
        if (response.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Your vote has been recorded!' });
          // Optimistically update the UI
          if (!comment.commentVote) {
            comment.commentVote = { voteUpCount: 0, voteDownCount: 0 };
          }
          if (voteValue === 'up') {
            comment.commentVote.voteUpCount = (comment.commentVote.voteUpCount ?? 0) + 1;
          } else {
            comment.commentVote.voteDownCount = (comment.commentVote.voteDownCount ?? 0) + 1;
          }
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
    } else { // 'comments'
      this.commentsError = fullMessage;
      this.isLoadingComments = false;
    }
    console.error(`Error (${type}): ${fullMessage}`);
  }

  goToCommentsPage(pageNumber: number): void {
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

  onCommentsSortChange(field: string): void {
    if (!this.discussionId) {
        return;
    }
    let direction = 'ASC';
    if (this.commentsSortField === field) {
      direction = this.commentsSortDirection === 'ASC' ? 'DESC' : 'ASC';
    }
    if (this.commentsSortField === field && this.commentsSortDirection === direction) {
         return;
    }
    this.commentsSortField = field;
    this.commentsSortDirection = direction;
    this.currentCommentsPage = 0;
    this.fetchComments(this.discussionId, this.currentCommentsPage, this.commentsPageSize, this.commentsSortField, this.commentsSortDirection);
  }

  private _calculateDisplayablePageNumbers(pageData: Page<any> | null): number[] {
    if (!pageData || typeof pageData.totalPages !== 'number' || pageData.totalPages <= 0) {
      return [];
    }
    const totalPages = pageData.totalPages;
    const currentPage = pageData.number;
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
        if (startPage > 1) pages.push(-1);
      }
      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }
      if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) pages.push(-1);
        pages.push(totalPages - 1);
      }
    }
    return pages;
  }

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

  /**
   * Formats FileInfoDTO array into a structure suitable for PrimeNG Galleria.
   * @param files The array of FileInfoDTO.
   * @returns An array of objects with 'image', 'thumbnail', 'alt', 'title' properties.
   */
  private formatImagesForGalleria(files: FileInfoDTO[] | null | undefined): any[] {
    if (!files || files.length === 0) {
      return [];
    }
    return files.map(file => ({
      image: `/api/public/files/${file.id}`, // URL for the full image
      thumbnail: `/api/public/files/${file.id}`, // URL for the thumbnail (can be same if no dedicated thumbnail endpoint)
      alt: file.originalFilename, // Alt text for accessibility
      title: file.originalFilename // Title for caption
    }));
  }

  /**
   * Opens the Galleria dialog for discussion images.
   */
  openDiscussionGalleria(): void {
    if (this.discussionDetails?.images && this.discussionDetails.images.length > 0) {
      this.currentGalleriaImages = this.formatImagesForGalleria(this.discussionDetails.images);
      this.galleriaVisible = true;
    }
  }

  /**
   * Opens the Galleria dialog for comment images.
   * @param comment The CommentDTO containing the images.
   */
  openCommentGalleria(comment: CommentDTO): void {
    if (comment.images && comment.images.length > 0) {
      this.currentGalleriaImages = this.formatImagesForGalleria(comment.images);
      this.galleriaVisible = true;
    }
  }

  /**
   * Closes the Galleria dialog.
   */
  closeGalleria(): void {
    this.galleriaVisible = false;
    this.currentGalleriaImages = []; // Clear images when closing
  }
}
