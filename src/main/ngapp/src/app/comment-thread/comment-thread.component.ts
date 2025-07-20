import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Subscription, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

// Services
import { CommentService } from '../_services/comment.service';
import { AvatarService } from '../_services/avatar.service';

// DTOs and Components
import { CommentThreadDTO, FileInfoDTO } from '../_data/dtos';
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
  private commentService = inject(CommentService);
  private avatarService = inject(AvatarService);
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

  // Galleria Methods (copied and adapted from DiscussionViewComponent)
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
      thumbnail: `/api/public/files/${file.id}`, // URL for the thumbnail
      alt: file.originalFilename,
      title: file.originalFilename
    }));
  }

  /**
   * Opens the Galleria dialog for a comment's images.
   * @param images The array of FileInfoDTOs from the comment.
   */
  openGalleria(images: FileInfoDTO[] | null | undefined): void {
    if (images && images.length > 0) {
      this.currentGalleriaImages = this.formatImagesForGalleria(images);
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

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }
}
