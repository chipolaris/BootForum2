import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageService } from 'primeng/api';
import { AdminChartService } from '../../_services/admin-chart.service';
import { AdminChartDTO, ChartDataDTO, errorMessageFromApiResponse } from '../../_data/dtos'; // ADDED ChartDataDTO
import { Subscription } from 'rxjs';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-admin-charts',
  standalone: true,
  imports: [CommonModule, ChartModule, SkeletonModule, ToastModule],
  templateUrl: './admin-charts.component.html',
})
export class AdminChartsComponent implements OnInit, OnDestroy {

  private chartService = inject(AdminChartService);
  private messageService = inject(MessageService);
  private subscriptions: Subscription[] = []; // Use an array for multiple subscriptions

  isLoading = true;
  error: string | null = null;

  contentActivityData: any;
  contentActivityOptions: any;

  newUsersData: any;
  newUsersOptions: any;

  forumActivityData: any;
  forumActivityOptions: any;

  // State for on-demand chart
  topTermsData: any;
  topTermsOptions: any;
  isTopTermsLoading = false;
  topTermsLoaded = false;

  ngOnInit(): void {
    this.loadInitialChartData();
  }

  loadInitialChartData(): void {
    this.isLoading = true;
    this.error = null;

    const sub = this.chartService.getChartData().subscribe({
      next: response => {
        if (response.success && response.data) {
          this.setupInitialCharts(response.data);
        } else {
          const detailMessage = errorMessageFromApiResponse(response) || 'Failed to load chart data.';
          this.error = detailMessage;
          this.messageService.add({ severity: 'error', summary: 'Error', detail: detailMessage });
        }
        this.isLoading = false;
      },
      error: err => {
        const detailMessage = err.message || 'An unexpected server error occurred.';
        this.error = detailMessage;
        this.messageService.add({ severity: 'error', summary: 'Server Error', detail: detailMessage });
        this.isLoading = false;
      }
    });
    this.subscriptions.push(sub);
  }

  // NEW: Method to load top terms on demand
  loadTopTerms(): void {
    this.isTopTermsLoading = true;
    this.topTermsLoaded = true; // Mark as loaded to show skeleton/chart area

    const sub = this.chartService.getTopTermsChartData().subscribe({
      next: response => {
        if (response.success && response.data) {
          this.setupTopTermsChart(response.data);
        } else {
          const detailMessage = errorMessageFromApiResponse(response) || 'Failed to load top terms data.';
          this.messageService.add({ severity: 'error', summary: 'Error', detail: detailMessage });
        }
        this.isTopTermsLoading = false;
      },
      error: err => {
        const detailMessage = err.message || 'An unexpected server error occurred.';
        this.messageService.add({ severity: 'error', summary: 'Server Error', detail: detailMessage });
        this.isTopTermsLoading = false;
      }
    });
    this.subscriptions.push(sub);
  }

  // RENAMED: To reflect it only sets up the initial charts
  setupInitialCharts(data: AdminChartDTO): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--p-text-color');
    const textColorSecondary = documentStyle.getPropertyValue('--p-text-muted-color');
    const surfaceBorder = documentStyle.getPropertyValue('--p-content-border-color');

    // Content Activity Chart (Stacked Bar)
    this.contentActivityData = {
      labels: data.contentActivity.labels,
      datasets: [
        {
          label: data.contentActivity.datasets[0].label, // Discussions
          backgroundColor: documentStyle.getPropertyValue('--p-cyan-500'),
          borderColor: documentStyle.getPropertyValue('--p-cyan-500'),
          data: data.contentActivity.datasets[0].data
        },
        {
          label: data.contentActivity.datasets[1].label, // Comments
          backgroundColor: documentStyle.getPropertyValue('--p-orange-500'),
          borderColor: documentStyle.getPropertyValue('--p-orange-500'),
          data: data.contentActivity.datasets[1].data
        }
      ]
    };
    this.contentActivityOptions = {
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: {
        tooltip: { mode: 'index', intersect: false },
        legend: { labels: { color: textColor } }
      },
      scales: {
        x: {
          stacked: true,
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        },
        y: {
          stacked: true,
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        }
      }
    };

    // New Users Chart (Bar)
    this.newUsersData = {
      labels: data.newUsers.labels,
      datasets: [
        {
          label: data.newUsers.datasets[0].label,
          data: data.newUsers.datasets[0].data,
          backgroundColor: documentStyle.getPropertyValue('--p-green-500'),
          borderColor: documentStyle.getPropertyValue('--p-green-500'),
        }
      ]
    };
    this.newUsersOptions = {
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: { legend: { labels: { color: textColor } } },
      scales: {
        x: {
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        },
        y: {
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        }
      }
    };

    // Forum Activity Chart (Stacked Bar)
    this.forumActivityData = {
      labels: data.forumActivity.labels,
      datasets: [
        {
          label: data.forumActivity.datasets[0].label, // Discussions
          backgroundColor: documentStyle.getPropertyValue('--p-indigo-500'),
          borderColor: documentStyle.getPropertyValue('--p-indigo-500'),
          data: data.forumActivity.datasets[0].data
        },
        {
          label: data.forumActivity.datasets[1].label, // Comments
          backgroundColor: documentStyle.getPropertyValue('--p-teal-500'),
          borderColor: documentStyle.getPropertyValue('--p-teal-500'),
          data: data.forumActivity.datasets[1].data
        }
      ]
    };
    this.forumActivityOptions = {
      indexAxis: 'y', // Horizontal bar chart
      maintainAspectRatio: false,
      aspectRatio: 0.8,
      plugins: {
        tooltip: { mode: 'index', intersect: false },
        legend: { labels: { color: textColor } }
      },
      scales: {
        x: {
          stacked: true,
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        },
        y: {
          stacked: true,
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        }
      }
    };
  }

  // NEW: Separate setup method for the on-demand chart
  setupTopTermsChart(data: ChartDataDTO): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--p-text-color');
    const textColorSecondary = documentStyle.getPropertyValue('--p-text-muted-color');
    const surfaceBorder = documentStyle.getPropertyValue('--p-content-border-color');

    this.topTermsData = {
      labels: data.labels,
      datasets: [
        {
          label: data.datasets[0].label, // Discussions
          backgroundColor: documentStyle.getPropertyValue('--p-purple-500'),
          borderColor: documentStyle.getPropertyValue('--p-purple-500'),
          data: data.datasets[0].data
        },
        {
          label: data.datasets[1].label, // Comments
          backgroundColor: documentStyle.getPropertyValue('--p-pink-500'),
          borderColor: documentStyle.getPropertyValue('--p-pink-500'),
          data: data.datasets[1].data
        }
      ]
    };
    this.topTermsOptions = {
      indexAxis: 'y', // Horizontal bar chart
      maintainAspectRatio: false,
      aspectRatio: 0.5, // Adjust aspect ratio for a taller chart
      plugins: {
        tooltip: { mode: 'index', intersect: false },
        legend: { labels: { color: textColor } }
      },
      scales: {
        x: {
          stacked: true,
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        },
        y: {
          stacked: true,
          ticks: { color: textColorSecondary },
          grid: { color: surfaceBorder }
        }
      }
    };
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
}
