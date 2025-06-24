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
import { Subscription, distinctUntilChanged, tap, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MessageService } from 'primeng/api';

import { DiscussionService } from '../_services/discussion.service';
import { CommentService } from '../_services/comment.service';
import { VoteService } from '../_services/vote.service';
import { AuthenticationService } from '../_services/authentication.service';
import { DiscussionDTO, CommentDTO, FileInfoDTO, Page, ApiResponse } from '../_data/dtos';
import { FileListComponent } from '../file-list/file-list.component'

import { NgIcon, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';

import { MarkdownModule } from 'ngx-markdown';

// PrimeNG Modules for Galleria
import { GalleriaModule } from 'primeng/galleria'; // Import GalleriaModule
import { DialogModule } from 'primeng/dialog';     // Import DialogModule

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
    GalleriaModule, // Add GalleriaModule to imports
    DialogModule    // Add DialogModule to imports
  ],
  providers: [
    provideIcons(APP_ICONS)
  ],
  templateUrl: './discussion-view.component.html',
  styleUrls: ['./discussion-view.component.css']
})
export class DiscussionViewComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router); // Inject Router
  private discussionService = inject(DiscussionService);
  private commentService = inject(CommentService);
  private voteService = inject(VoteService);
  private authService = inject(AuthenticationService); // Inject AuthenticationService
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

  private subscriptions = new Subscription();

  // --- Galleria Properties ---
  galleriaVisible: boolean = false; // Controls the visibility of the Galleria dialog
  currentGalleriaImages: any[] = []; // Holds the images to display in the Galleria

  // Responsive options for Galleria (optional, but good for different screen sizes)
  responsiveOptions: any[] = [
    {
      breakpoint: '1024px',
      numVisible: 5
    },
    {
      breakpoint: '768px',
      numVisible: 3
    },
    {
      breakpoint: '560px',
      numVisible: 1
    }
  ];

  // --- No changes to other lifecycle hooks or data fetching methods ---

  ngOnInit(): void {
    const routeParamsSub = this.route.paramMap.pipe(
      tap(params => console.log('[OnInit] Route params changed', params.get('id'))),
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
    console.log('[resetDataStates] Resetting component data states.');
    this.discussionDetails = null;
    this.commentsPage = null;
    this.commentsDataForViewers = [];

    this.currentCommentsPage = 0;
    this.displayableCommentPageNumbers = [];

    this.isLoadingDiscussion = true;
    this.discussionError = null;

    this.isLoadingComments = true;
    this.commentsError = null;
  }

  private loadDiscussionData(discussionId: number): void {
    console.log(`[loadDiscussionData] Loading data for discussion ID: ${discussionId}`);
    this.fetchDiscussionDetails(discussionId);
    this.fetchComments(discussionId, 0, this.commentsPageSize, this.commentsSortField, this.commentsSortDirection);
  }

  fetchDiscussionDetails(id: number): void {
    this.isLoadingDiscussion = true;
    this.discussionError = null;
    const sub = this.discussionService.getDiscussionById(id).subscribe({
      next: (response: ApiResponse<DiscussionDTO>) => {
        if (response.success && response.data) {
          this.discussionDetails = response.data;
        } else {
          this.handleError(response.message || 'Failed to load discussion details.', 'discussion', response.errors);
        }
        this.isLoadingDiscussion = false;
      },
      error: (err) => this.handleError(err.message || 'Error fetching discussion.', 'discussion')
    });
    this.subscriptions.add(sub);
  }

  fetchComments(discussionId: number, pageForComments: number, size: number, sortProperty: string, sortDirection: string): void {
    console.log(`[fetchComments] Fetching comments for discussion ID: ${discussionId}, page: ${pageForComments} using NEW endpoint.`);
    this.isLoadingComments = true;
    this.commentsError = null;

    // No need for pageForBackend, pageForComments is directly used for the new endpoint
    const sub = this.commentService.listComments(discussionId, pageForComments, size, sortProperty, sortDirection)
      .pipe(catchError(err => {
        this.handleError(err.message || 'Error fetching comments.', 'comments');
        if (this.commentsDataForViewers.length > 0 || this.commentsPage !== null) {
            this.commentsDataForViewers = [];
            this.commentsPage = null;
            this.cdr.detectChanges();
        }
        return of(null);
      }))
      .subscribe(response => {
        let newCommentsData: CommentDTO[] = [];
        let newCommentsPage: Page<CommentDTO> | null = null;

        if (response && response.success && response.data) {
          // The data from listComments is already filtered by the backend
          // No client-side 'shift()' or processing of rawPageData is needed here.
          newCommentsPage = response.data;
          newCommentsData = newCommentsPage.content; // Directly use the content

          console.log('[fetchComments] Raw data received for replies (from new endpoint):', response.data);

        } else if (response && !response.success) {
          this.handleError(response.message || 'Failed to load comments.', 'comments', response.errors);
        }

        this.commentsPage = newCommentsPage;
        this.commentsDataForViewers = newCommentsData;
        this.currentCommentsPage = newCommentsPage ? newCommentsPage.number : 0;
        this.displayableCommentPageNumbers = this._calculateDisplayablePageNumbers(newCommentsPage);

        if (!this.commentsError) {
            this.isLoadingComments = false;
        }

        console.log(`[fetchComments] Updated commentsDataForViewers. Length: ${this.commentsDataForViewers.length}. isLoading: ${this.isLoadingComments}`);
        console.log('[Debug Before DetectChanges] isLoadingComments:', this.isLoadingComments,
                    'commentsError:', this.commentsError,
                    'commentsDataForViewers.length:', this.commentsDataForViewers.length,
                    'commentsPage:', this.commentsPage ? 'Exists' : 'null',
                    'commentsPage.empty:', this.commentsPage?.empty);

        this.cdr.detectChanges();
      });
    this.subscriptions.add(sub);
  }

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
