import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router'; // RouterModule for potential routerLink later
import { ForumService } from '../_services/forum.service'; // Adjust path as needed
import { ForumViewDTO } from '../_data/dtos'; // Adjust path as needed
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common'; // For DatePipe, ngIf, ngFor

@Component({
  selector: 'app-forum-view',
  standalone: true,
  imports: [
    CommonModule, // Provides *ngIf, *ngFor, DatePipe, etc.
    RouterModule  // If you plan to add routerLinks for discussion titles
  ],
  templateUrl: './forum-view.component.html',
  styleUrls: ['./forum-view.component.css']
})
export class ForumViewComponent implements OnInit, OnDestroy {
  forumViewData: ForumViewDTO | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  private routeSubscription: Subscription | undefined;
  private forumViewSubscription: Subscription | undefined;

  constructor(
    private route: ActivatedRoute,
    private forumService: ForumService
  ) {}

  ngOnInit(): void {
    this.routeSubscription = this.route.paramMap.subscribe(params => {
      const idParam = params.get('id');
      if (idParam) {
        const forumId = +idParam; // Convert string to number
        if (!isNaN(forumId)) {
          this.fetchForumView(forumId);
        } else {
          this.isLoading = false;
          this.errorMessage = 'Invalid Forum ID in route parameters.';
          console.error(this.errorMessage);
        }
      } else {
        this.isLoading = false;
        this.errorMessage = 'Forum ID not found in route parameters.';
        console.error(this.errorMessage);
      }
    });
  }

  fetchForumView(id: number): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.forumViewSubscription = this.forumService.getForumView(id).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.forumViewData = response.data;
        } else {
          this.errorMessage = response.message || 'Failed to load forum data. The forum might not exist or an error occurred.';
          // Log the full error details if available from the backend
          if (response.errors) {
            console.error('Error details:', response.errors);
          }
        }
        this.isLoading = false;
      },
      error: (err) => {
        // The handleError in your service already formats the error message
        this.errorMessage = err.message || 'An unexpected error occurred while fetching forum data.';
        console.error('HTTP error fetching forum view:', err);
        this.isLoading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
    this.forumViewSubscription?.unsubscribe();
  }
}
