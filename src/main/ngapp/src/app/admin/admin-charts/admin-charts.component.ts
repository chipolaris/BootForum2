import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { MessageService } from 'primeng/api';
import { AdminChartService } from '../../_services/admin-chart.service';
import { AdminChartDTO, errorMessageFromApiResponse } from '../../_data/dtos';
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
  private subscription!: Subscription;

  isLoading = true;
  error: string | null = null;

  contentActivityData: any;
  contentActivityOptions: any;

  newUsersData: any;
  newUsersOptions: any;

  forumActivityData: any;
  forumActivityOptions: any;

  ngOnInit(): void {
    this.loadChartData();
  }

  loadChartData(): void {
    this.isLoading = true;
    this.error = null;

    this.subscription = this.chartService.getChartData().subscribe({
      next: response => {
        if (response.success && response.data) {
          this.setupCharts(response.data);
        } else {
          // FIX: Use a local constant for the message to ensure correct typing.
          const detailMessage = errorMessageFromApiResponse(response) || 'Failed to load chart data.';
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

  setupCharts(data: AdminChartDTO): void {
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

  ngOnDestroy(): void {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
  }
}
