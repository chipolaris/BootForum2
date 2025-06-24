import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subscription, switchMap, tap } from 'rxjs';
import { UserService } from '../_services/user.service';
import { UserProfileDTO } from '../_data/dtos';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, NgIcon],
  providers: [provideIcons(APP_ICONS)],
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.css']
})
export class UserProfileComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private userService = inject(UserService);
  private subscription = new Subscription();

  userProfile: UserProfileDTO | null = null;
  isLoading = true;
  error: string | null = null;

  ngOnInit(): void {
    const profileSub = this.route.paramMap.pipe(
      tap(() => {
        this.isLoading = true;
        this.error = null;
        this.userProfile = null;
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
        } else {
          this.error = response.message || 'Failed to load user profile.';
        }
        this.isLoading = false;
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

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }
}
