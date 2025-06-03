import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  ViewChild,
  ViewChildren,
  ElementRef,
  QueryList,
  AfterViewInit,
  ChangeDetectorRef
} from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, distinctUntilChanged, switchMap, tap } from 'rxjs';

import { DiscussionService } from '../_services/discussion.service';
import { CommentService } from '../_services/comment.service';
import { DiscussionDTO, CommentDTO, Page, ApiResponse } from '../_data/dtos';

// Import Toast UI Viewer
import Viewer from '@toast-ui/editor/dist/toastui-editor-viewer'; // Correct import path for Viewer

// Import NgIconComponent and provideIcons if you plan to use icons
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons'; // Assuming you have this

@Component({
  selector: 'app-discussion-view',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    NgIconComponent, // Add if using icons
    DatePipe
  ],
  providers: [
    provideIcons(APP_ICONS) // Add if using icons
  ],
  templateUrl: './discussion-view.component.html',
  styleUrls: ['./discussion-view.component.css']
})
export class DiscussionViewComponent implements OnInit, OnDestroy, AfterViewInit {
  private route = inject(ActivatedRoute);
  private discussionService = inject(DiscussionService);
  private commentService = inject(CommentService);
  private cdr = inject(ChangeDetectorRef); // Inject ChangeDetectorRef

  discussionId: number | null = null;
  discussionDetails: DiscussionDTO | null = null;
  commentsPage: Page<CommentDTO> | null = null;

  private _firstComment: CommentDTO | null = null;
  get firstComment(): CommentDTO | null {
    return this._firstComment;
  }

  // Inside the DiscussionViewComponent class
  set firstComment(comment: CommentDTO | null) {
    this._firstComment = comment;
    if (comment && this.firstCommentViewerElRef?.nativeElement) {
      // ***** FIX HERE *****
      // Pass 'true' for the isFirstComment parameter
      this.initializeViewer(this.firstCommentViewerElRef.nativeElement, comment.content, this.firstCommentViewerInstance, true);
    } else if (!comment && this.firstCommentViewerInstance) {
        this.firstCommentViewerInstance.destroy();
        this.firstCommentViewerInstance = null;
    }
  }


  isLoadingDiscussion = true;
  isLoadingComments = true;
  discussionError: string | null = null;
  commentsError: string | null = null;

  // Pagination for comments
  currentCommentsPage = 0;
  commentsPageSize = 10; // Or your preferred default
  commentsSortField: string = 'createDate';
  commentsSortDirection: string = 'ASC';
  displayableCommentPageNumbers: number[] = [];

  private subscriptions = new Subscription();

  // Toast UI Viewer instances
  @ViewChild('firstCommentViewerEl') private firstCommentViewerElRef!: ElementRef<HTMLDivElement>;
  private firstCommentViewerInstance: InstanceType<typeof Viewer> | null = null;

  @ViewChildren('followUpCommentViewerEl') private followUpCommentViewerElRefs!: QueryList<ElementRef<HTMLDivElement>>;
  private followUpCommentViewerInstances: InstanceType<typeof Viewer>[] = [];
  commentsDataForViewers: CommentDTO[] = [];


  ngOnInit(): void {
    const routeParamsSub = this.route.paramMap.pipe(
      tap(params => console.log('DiscussionViewComponent: Route params changed', params.get('id'))),
      distinctUntilChanged((prev, curr) => prev.get('id') === curr.get('id'))
    ).subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        const parsedDiscussionId = +idParam;
        if (!isNaN(parsedDiscussionId)) {
          if (this.discussionId !== parsedDiscussionId) {
            this.discussionId = parsedDiscussionId;
            this.currentCommentsPage = 0; // Reset page for new discussion
            this.fetchDiscussionDetails(this.discussionId);
            this.fetchComments(this.discussionId, this.currentCommentsPage, this.commentsPageSize, this.commentsSortField, this.commentsSortDirection);
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

  ngAfterViewInit(): void {
    // Initialize first comment viewer if data and element are ready
    if (this.firstComment && this.firstCommentViewerElRef?.nativeElement) {
      this.initializeViewer(this.firstCommentViewerElRef.nativeElement, this.firstComment.content, this.firstCommentViewerInstance, true);
    }

    // Subscribe to changes in the followUpCommentViewerElRefs QueryList
    // This helps in initializing viewers when new comment elements are rendered by *ngFor
    const followUpViewersSub = this.followUpCommentViewerElRefs.changes.subscribe(() => {
        this.setupFollowUpCommentViewers();
    });
    this.subscriptions.add(followUpViewersSub);
    this.setupFollowUpCommentViewers(); // Initial setup
  }

  private initializeViewer(element: HTMLDivElement, content: string, existingViewer: InstanceType<typeof Viewer> | null, isFirstComment: boolean = false): void {
    if (existingViewer && !isFirstComment) { // For follow-up, we destroy and recreate
        existingViewer.destroy();
    } else if (isFirstComment && this.firstCommentViewerInstance) { // For first comment, update or destroy/recreate
        this.firstCommentViewerInstance.setMarkdown(content);
        return;
    }

    try {
      const newViewer = new Viewer({
        el: element,
        initialValue: content
      });
      if (isFirstComment) {
        this.firstCommentViewerInstance = newViewer;
      } else {
        // For follow-up comments, the management is now in setupFollowUpCommentViewers
      }
    } catch (e) {
        console.error('Error initializing Toast UI Viewer:', e, 'for element:', element);
    }
  }

  private setupFollowUpCommentViewers(): void {
    // Destroy existing follow-up viewers
    this.followUpCommentViewerInstances.forEach(viewer => viewer.destroy());
    this.followUpCommentViewerInstances = [];

    // Ensure elements and data are available
    if (this.followUpCommentViewerElRefs && this.commentsDataForViewers.length > 0) {
      this.followUpCommentViewerElRefs.toArray().forEach((elRef, index) => {
        if (this.commentsDataForViewers[index] && elRef?.nativeElement) {
          try {
            const viewer = new Viewer({
              el: elRef.nativeElement,
              initialValue: this.commentsDataForViewers[index].content
            });
            this.followUpCommentViewerInstances.push(viewer);
          } catch (e) {
            console.error('Error initializing follow-up Toast UI Viewer:', e, 'for comment:', this.commentsDataForViewers[index].id);
          }
        }
      });
    }
  }


  fetchDiscussionDetails(id: number): void {
    this.isLoadingDiscussion = true;
    this.discussionError = null;
    const discussionSub = this.discussionService.getDiscussionById(id).subscribe({
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
    this.subscriptions.add(discussionSub);
  }

  fetchComments(discussionId: number, page: number, size: number, sortProperty: string, sortDirection: string): void {
    this.isLoadingComments = true;
    this.commentsError = null;
    this.firstComment = null; // Reset first comment, its viewer will be handled by setter

    // Destroy existing follow-up viewers before fetching new comments
    this.followUpCommentViewerInstances.forEach(viewer => viewer.destroy());
    this.followUpCommentViewerInstances = [];
    this.commentsDataForViewers = [];


    const commentsSub = this.commentService.listComments(discussionId, page, size, sortProperty, sortDirection)
      .subscribe({
        next: (response: ApiResponse<Page<CommentDTO>>) => {
          if (response.success && response.data) {
            this.commentsPage = response.data;
            if (this.commentsPage.number === 0 && this.commentsPage.content.length > 0) {
              this.firstComment = this.commentsPage.content[0]; // Setter will handle viewer
            }
            // Prepare data for follow-up viewers
            this.commentsDataForViewers = this.firstComment
                                          ? this.commentsPage.content.slice(1)
                                          : this.commentsPage.content;

            this.currentCommentsPage = this.commentsPage.number;
            this.displayableCommentPageNumbers = this._calculateDisplayablePageNumbers(this.commentsPage);

            // Trigger change detection to render new comment elements, then setup viewers
            this.cdr.detectChanges();
            this.setupFollowUpCommentViewers();

          } else {
            this.handleError(response.message || 'Failed to load comments.', 'comments', response.errors);
            this.displayableCommentPageNumbers = [];
          }
          this.isLoadingComments = false;
        },
        error: (err) => {
          this.handleError(err.message || 'Error fetching comments.', 'comments');
          this.displayableCommentPageNumbers = [];
          this.isLoadingComments = false;
        }
      });
    this.subscriptions.add(commentsSub);
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

  // --- Comment Pagination Logic ---
  goToCommentPage(pageNumber: number): void {
    if (this.discussionId === null || pageNumber < 0 || (this.commentsPage && pageNumber >= this.commentsPage.totalPages)) {
      return;
    }
    this.fetchComments(this.discussionId, pageNumber, this.commentsPageSize, this.commentsSortField, this.commentsSortDirection);
  }

  onCommentsPageSizeChange(newSizeValue: string | number): void {
    const newSize = +newSizeValue;
    this.commentsPageSize = newSize;
    this.currentCommentsPage = 0;
    if (this.discussionId !== null) {
      this.fetchComments(this.discussionId, this.currentCommentsPage, this.commentsPageSize, this.commentsSortField, this.commentsSortDirection);
    }
  }

  onCommentsSortChange(field: string): void {
    if (!this.discussionId) return;
    let direction = 'ASC';
    if (this.commentsSortField === field) {
      direction = this.commentsSortDirection === 'ASC' ? 'DESC' : 'ASC';
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
    const maxPagesToShow = 5; // Number of page links to show
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
        pages.push(0); // Always show first page
        if (startPage > 1) pages.push(-1); // Ellipsis indicator
      }

      for (let i = startPage; i <= endPage; i++) {
        pages.push(i);
      }

      if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) pages.push(-1); // Ellipsis indicator
        pages.push(totalPages - 1); // Always show last page
      }
    }
    return pages;
  }

  public getObjectKeys(obj: any): string[] {
    return obj ? Object.keys(obj) : [];
  }


  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    // Destroy Toast UI Viewer instances
    if (this.firstCommentViewerInstance) {
      this.firstCommentViewerInstance.destroy();
    }
    this.followUpCommentViewerInstances.forEach(viewer => viewer.destroy());
  }
}
