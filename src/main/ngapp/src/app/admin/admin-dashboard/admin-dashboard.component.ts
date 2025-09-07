import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminDashboardService } from '../../_services/admin-dashboard.service';
import { AdminDashboardDTO, RankedListItemDTO, SnapshotStatsDTO, errorMessageFromApiResponse } from '../../_data/dtos';
import { MessageService } from 'primeng/api';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../../shared/hero-icons';
import { SelectButtonModule } from 'primeng/selectbutton';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NgIcon, SelectButtonModule, FormsModule, SkeletonModule],
  providers: [provideIcons(APP_ICONS)],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {

  private dashboardService = inject(AdminDashboardService);
  private messageService = inject(MessageService);

  isLoading = true;
  dashboardData: AdminDashboardDTO | null = null;
  error: string | null = null;

  timeWindowOptions: any[] = [
    { label: 'Last 7 Days', value: '7d' },
    { label: 'Last 30 Days', value: '30d' },
    { label: 'Last Year', value: '1y' },
    { label: 'All Time', value: 'all' }
  ];
  selectedTimeWindow: string = 'all';

  // For skeleton loading
  dummyRankedList = new Array(5);

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;
    this.error = null;
    this.dashboardService.getDashboardData(this.selectedTimeWindow).subscribe({
      next: response => {
        if (response.success && response.data) {
          this.dashboardData = response.data;
        } else {
          // FIX: Use a local constant for the message to ensure correct typing.
          const detailMessage = errorMessageFromApiResponse(response) || 'Failed to load dashboard data.';
          this.error = detailMessage;
          this.messageService.add({ severity: 'error', summary: 'Error', detail: detailMessage });
        }
        this.isLoading = false;
      },
      error: err => {
        // FIX: Use a local constant for the message to ensure correct typing.
        const detailMessage = err.message || 'An unexpected server error occurred.';
        this.error = detailMessage;
        this.messageService.add({ severity: 'error', summary: 'Server Error', detail: detailMessage });
        this.isLoading = false;
      }
    });
  }

  onTimeWindowChange(): void {
    this.loadDashboardData();
  }

  getRouterLink(item: RankedListItemDTO, type: string): any[] {
    switch (type) {
      case 'user': return ['/app/users', item.name, 'profile'];
      case 'discussion': return ['/app/discussions', item.id, 'view'];
      case 'forum': return ['/app/forums', item.id, 'view'];
      case 'tag': return ['/app/discussions/by-tag']; // Query params handled in template
      default: return [];
    }
  }
}
