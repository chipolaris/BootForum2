import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { Subscription, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { MessageService } from 'primeng/api';

// Services
import { CommentService } from '../_services/comment.service';
import { AvatarService } from '../_services/avatar.service';
import { VoteService } from '../_services/vote.service';
import { AuthenticationService } from '../_services/authentication.service';

// DTOs and Components
import { CommentThreadDTO, FileInfoDTO, CommentDTO, DiscussionDTO } from '../_data/dtos';
import { FileListComponent } from '../file-list/file-list.component';

// UI Modules
import { NgIcon, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';
import { MarkdownModule } from 'ngx-markdown';
import { GalleriaModule } from 'primeng/galleria';
import { DialogModule } from 'primeng/dialog';

@Component({
  selector: 'app-comment-thread',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    NgIcon,
    MarkdownModule,
    FileListComponent,
    GalleriaModule,
    DialogModule
  ],
  providers: [provideIcons(APP_ICONS)],
  templateUrl: './comment-thread.component.html',
  styleUrls: ['./comment-thread.component.css']
})
export class CommentThreadComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private commentService = inject(CommentService);
  private voteService = inject(VoteService);
  private authService = inject(AuthenticationService);
  private avatarService = inject(AvatarService);
  private messageService = inject(MessageService);
  private cdr = inject(ChangeDetectorRef);

  isLoading = true;
  error: string | null = null;
  commentThread: CommentThreadDTO | null = null;
  avatarFileIdMap: Map<string, number | null> = new Map();

  private subscriptions = new Subscription();

  // Galleria Properties
  galleriaVisible = false;
  currentGalleriaImages: any[] = [];
  responsiveOptions: any[] = [
    { breakpoint: '1024px', numVisible: 5 },
    { breakpoint: '768px', numVisible: 3 },
    { breakpoint: '560px', numVisible: 1 }
  ];

  ngOnInit(): void {
    const routeSub = this.route.paramMap.subscribe(params => {
      const commentIdParam = params.get('id');
      if (commentIdParam) {
        const commentId = +commentIdParam;
        this.loadCommentThread(commentId);
      } else {
        this.error = 'Comment ID not found in route.';
        this.isLoading = false;
      }
    });
    this.subscriptions.add(routeSub);
  }

  loadCommentThread(commentId: number): void {
    this.isLoading = true;
    this.error = null;

    const threadSub = this.commentService.getCommentThread(commentId)
      .pipe(catchError(err => {
        this.error = err.message || 'An unexpected error occurred while fetching the comment thread.';
        this.isLoading = false;
        return of(null);
      }))
      .subscribe(response => {
        if (response?.success && response.data) {
          this.commentThread = response.data;
          this.fetchAvatarFileIds();
        } else if (response) {
          this.error = response.message || 'Failed to load comment thread.';
        }
        this.isLoading = false;
      });

    this.subscriptions.add(threadSub);
  }

  private fetchAvatarFileIds(): void {
    if (!this.commentThread) return;

    const usernames = new Set<string>();
    usernames.add(this.commentThread.discussionDTO.createBy);
    this.commentThread.commentDTOs.forEach(comment => usernames.add(comment.createBy));

    if (usernames.size === 0) return;

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

  voteForDiscussion(discussion: DiscussionDTO, voteValue: 'up' | 'down'): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/app/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    if (!discussion.id) return;
    this.voteService.voteOnDiscussion(discussion.id, voteValue).subscribe({
      next: (response) => {
        if (response.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Your vote has been recorded!' });
          if (discussion.stat) {
            if (voteValue === 'up') discussion.stat.voteUpCount = (discussion.stat.voteUpCount ?? 0) + 1;
            else discussion.stat.voteDownCount = (discussion.stat.voteDownCount ?? 0) + 1;
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

  // Galleria Methods
  private formatImagesForGalleria(files: FileInfoDTO[] | null | undefined): any[] {
    if (!files || files.length === 0) {
      return [];
    }
    return files.map(file => ({
      image: `/api/public/files/${file.id}`,
      thumbnail: `/api/public/files/${file.id}`,
      alt: file.originalFilename,
      title: file.originalFilename
    }));
  }

  openGalleria(images: FileInfoDTO[] | null | undefined): void {
    if (images && images.length > 0) {
      this.currentGalleriaImages = this.formatImagesForGalleria(images);
      this.galleriaVisible = true;
    }
  }

  closeGalleria(): void {
    this.galleriaVisible = false;
    this.currentGalleriaImages = [];
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }
}
