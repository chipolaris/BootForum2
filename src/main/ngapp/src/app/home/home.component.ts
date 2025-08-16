import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { DiscussionService } from '../_services/discussion.service';
import { AvatarService } from '../_services/avatar.service';
import { DiscussionDTO, errorMessageFromApiResponse } from '../_data/dtos';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, NgIconComponent],
  providers: [provideIcons(APP_ICONS)],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  private discussionService = inject(DiscussionService);
  private avatarService = inject(AvatarService);
  private cdr = inject(ChangeDetectorRef);

  latestDiscussions: DiscussionDTO[] = [];
  mostCommentedDiscussions: DiscussionDTO[] = [];
  mostViewedDiscussions: DiscussionDTO[] = [];

  avatarFileIdMap: Map<string, number | null> = new Map();

  isLoading = true;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadHomePageData();
  }

  loadHomePageData(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.avatarFileIdMap.clear();

    const latest$ = this.discussionService.getLatestDiscussions();
    const mostCommented$ = this.discussionService.getMostCommentedDiscussions();
    const mostViewed$ = this.discussionService.getMostViewedDiscussions();

    forkJoin({
      latest: latest$,
      commented: mostCommented$,
      viewed: mostViewed$
    }).subscribe({
      next: (responses) => {
        let errors: string[] = [];

        if (responses.latest.success && responses.latest.data) {
          this.latestDiscussions = responses.latest.data;
        } else {
          errors.push(errorMessageFromApiResponse(responses.latest) || 'Failed to load latest discussions.');
        }

        if (responses.commented.success && responses.commented.data) {
          this.mostCommentedDiscussions = responses.commented.data;
        } else {
          errors.push(errorMessageFromApiResponse(responses.commented) || 'Failed to load most commented discussions.');
        }

        if (responses.viewed.success && responses.viewed.data) {
          this.mostViewedDiscussions = responses.viewed.data;
        } else {
          errors.push(errorMessageFromApiResponse(responses.viewed) || 'Failed to load most viewed discussions.');
        }

        if(errors.length > 0) {
          this.errorMessage = errors.join('\n');
        }

        this.fetchAvatarFileIds();
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message || 'An unexpected error occurred while fetching home page data.';
        this.isLoading = false;
      }
    });
  }

  private fetchAvatarFileIds(): void {
    const usernames = new Set<string>();
    this.latestDiscussions.forEach(d => usernames.add(d.createBy));
    this.mostCommentedDiscussions.forEach(d => usernames.add(d.createBy));
    this.mostViewedDiscussions.forEach(d => usernames.add(d.createBy));

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
      error: (err) => console.error('Failed to fetch avatar file IDs for home page:', err)
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
}
