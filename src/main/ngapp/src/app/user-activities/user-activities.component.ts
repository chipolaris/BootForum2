import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { UserActivitiesService } from '../_services/user-activities.service';
import { MyActivitiesDTO, errorMessageFromApiResponse } from '../_data/dtos';
import { PanelModule } from 'primeng/panel';
import { TableModule } from 'primeng/table';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';

@Component({
  selector: 'app-my-activities',
  standalone: true,
  imports: [CommonModule, RouterModule, PanelModule, TableModule, NgIcon],
  providers: [provideIcons(APP_ICONS)],
  templateUrl: './user-activities.component.html',
  styleUrls: ['./user-activities.component.css']
})
export class UserActivitiesComponent implements OnInit {
  private userActivitiesService = inject(UserActivitiesService);

  activities: MyActivitiesDTO | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadActivities();
  }

  loadActivities(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.userActivitiesService.getMyActivities().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.activities = response.data;
        } else {
          this.errorMessage = errorMessageFromApiResponse(response) || 'Failed to load your activities.';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message || 'An unexpected error occurred while fetching your activities.';
        this.isLoading = false;
      }
    });
  }
}
