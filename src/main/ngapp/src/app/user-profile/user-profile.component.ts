import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subscription, switchMap, tap, of } from 'rxjs';
import { UserService } from '../_services/user.service';
import { AvatarService } from '../_services/avatar.service'; // <-- IMPORT
import { UserProfileDTO } from '../_data/dtos';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { MarkdownModule } from 'ngx-markdown';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, MarkdownModule],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.css']
})
export class UserProfileComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private userService = inject(UserService);
  private avatarService = inject(AvatarService);
  private subscription = new Subscription();

  userProfile: UserProfileDTO | null = null;
  avatarFileId: number | null = null;
  isLoading = true;
  error: string | null = null;

  ngOnInit(): void {
    const profileSub = this.route.paramMap.pipe(
      tap(() => {
        this.isLoading = true;
        this.error = null;
        this.userProfile = null;
        this.avatarFileId = null; // RESET
      }),
      switchMap(params => {
        const username = params.get('username');
        if (!username) {
          this.router.navigate(['/app/not-found']);
          throw new Error('Username not found in route');
        }
        return this.userService.getUserProfile(username);
      })
    ).subscribe({
      next: response => {
        if (response.success && response.data) {
          this.userProfile = response.data;
          // After successfully getting the profile, fetch the avatar ID
          this.fetchAvatarId(this.userProfile.username);
        } else {
          this.error = response.message || 'Failed to load user profile.';
          this.isLoading = false; // Stop loading on failure
        }
        // Note: isLoading will be set to false inside fetchAvatarId or in the error block
      },
      error: err => {
        console.error('Error fetching user profile:', err);
        if (err.status === 404) {
          this.error = `User profile not found.`;
        } else {
          this.error = 'An unexpected error occurred while fetching the user profile.';
        }
        this.isLoading = false;
      }
    });

    this.subscription.add(profileSub);
  }

  /**
   * Fetches the avatar file ID for the current user profile.
   */
  private fetchAvatarId(username: string): void {
    const avatarSub = this.avatarService.getAvatarFileId(username).subscribe({
      next: (response) => {
        if (response.success) {
          this.avatarFileId = response.data ?? null;
        }
        // Whether avatar is found or not, the main loading is complete.
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Failed to fetch avatar ID, will use default.", err);
        // Even if avatar fails, don't block the page from rendering.
        this.isLoading = false;
      }
    });
    this.subscription.add(avatarSub);
  }

  /**
   * Generates the correct avatar URL using the fetched file ID.
   */
  getAvatarUrl(): string {
    if (this.avatarFileId) {
      return `/api/public/files/${this.avatarFileId}`;
    }
    return '/assets/images/default-avatar.png';
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }
}
