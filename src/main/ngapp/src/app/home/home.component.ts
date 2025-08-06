import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { DiscussionService } from '../_services/discussion.service';
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

  latestDiscussions: DiscussionDTO[] = [];
  mostCommentedDiscussions: DiscussionDTO[] = [];
  mostViewedDiscussions: DiscussionDTO[] = [];

  isLoading = true;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadHomePageData();
  }

  loadHomePageData(): void {
    this.isLoading = true;
    this.errorMessage = null;

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

        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message || 'An unexpected error occurred while fetching home page data.';
        this.isLoading = false;
      }
    });
  }
}
