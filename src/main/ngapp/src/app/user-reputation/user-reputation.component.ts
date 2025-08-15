import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { UserReputationService } from '../_services/user-reputation.service';
import { UserReputationDTO, errorMessageFromApiResponse } from '../_data/dtos';
import { PanelModule } from 'primeng/panel';
import { TableModule } from 'primeng/table';
import { CardModule } from 'primeng/card';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';

@Component({
  selector: 'app-user-reputation',
  standalone: true,
  imports: [CommonModule, RouterModule, PanelModule, TableModule, CardModule, NgIcon],
  providers: [provideIcons(APP_ICONS)],
  templateUrl: './user-reputation.component.html',
  styleUrls: ['./user-reputation.component.css']
})
export class UserReputationComponent implements OnInit {
  private userReputationService = inject(UserReputationService);

  reputation: UserReputationDTO | null = null;
  isLoading = true;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadReputation();
  }

  loadReputation(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.userReputationService.getMyReputation().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.reputation = response.data;
        } else {
          this.errorMessage = errorMessageFromApiResponse(response) || 'Failed to load your reputation data.';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.message || 'An unexpected error occurred while fetching your reputation data.';
        this.isLoading = false;
      }
    });
  }
}
