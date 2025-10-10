import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // ADDED
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageService } from 'primeng/api';
import { AdminChartService } from '../../_services/admin-chart.service';
import { AdminChartDTO, ChartDataDTO, errorMessageFromApiResponse } from '../../_data/dtos';
import { Subscription } from 'rxjs';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-admin-charts',
  standalone: true,
  imports: [CommonModule, FormsModule, ChartModule, SkeletonModule, ToastModule], // ADDED FormsModule
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
  topTermsChartHeight: number = 600; // NEW: For dynamic chart height, with a default

  // Properties for the filter controls
  topTermsLimit: number = 25;
  topTermsPeriod: string = 'all';

  limitOptions = [
    { label: 'Top 25', value: 25 },
    { label: 'Top 50', value: 50 },
    { label: 'Top 100', value: 100 }
  ];

  periodOptions = [
    { label: 'All Time', value: 'all' },
    { label: 'Last Year', value: 'year' },
    { label: 'Last Month', value: 'month' },
    { label: 'Last Week', value: 'week' }
  ];

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

  // MODIFIED: Method to load top terms on demand, now with height calculation
  loadTopTerms(): void {
    this.isTopTermsLoading = true;
    this.topTermsLoaded = true; // Mark as loaded to show skeleton/chart area

    // NEW: Pre-calculate an estimated height for the skeleton to avoid layout shifts
    const pixelsPerBar = 24; // Adjust as needed for good spacing
    const chartPadding = 100; // For top/bottom padding, axes, etc.
    const estimatedHeight = (this.topTermsLimit * pixelsPerBar) + chartPadding;
    this.topTermsChartHeight = Math.max(400, estimatedHeight); // Use a minimum height

    const sub = this.chartService.getTopTermsChartData(this.topTermsLimit, this.topTermsPeriod).subscribe({
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

  // MODIFIED: Separate setup method for the on-demand chart, now with height calculation
  setupTopTermsChart(data: ChartDataDTO): void {
    const documentStyle = getComputedStyle(document.documentElement);
    const textColor = documentStyle.getPropertyValue('--p-text-color');
    const textColorSecondary = documentStyle.getPropertyValue('--p-text-muted-color');
    const surfaceBorder = documentStyle.getPropertyValue('--p-content-border-color');

    // NEW: Calculate final height based on actual data received
    const pixelsPerBar = 24;
    const chartPadding = 100;
    // Use the actual number of labels returned by the API
    const finalHeight = (data.labels.length * pixelsPerBar) + chartPadding;
    this.topTermsChartHeight = Math.max(400, finalHeight);

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
      // aspectRatio is less important now that we control the height dynamically
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
