import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PanelModule } from 'primeng/panel';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MessageModule } from 'primeng/message';

import { HealthCheckService } from '../_services/health-check.service';
import { ApplicationHealthDTO } from '../_data/dtos';

@Component({
  selector: 'app-health-check',
  standalone: true,
  imports: [
    CommonModule,
    PanelModule,
    TagModule,
    ProgressSpinnerModule,
    MessageModule
  ],
  templateUrl: './health-check.component.html',
  styleUrls: ['./health-check.component.css']
})
export class HealthCheckComponent implements OnInit {

  private healthCheckService = inject(HealthCheckService);

  healthStatus: ApplicationHealthDTO | null = null;
  isLoading = true;
  error: string | null = null;

  ngOnInit(): void {
    this.healthCheckService.checkHealth().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.healthStatus = response.data;
        } else {
          this.error = response.message || 'Failed to retrieve health status.';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.error = err.message || 'An unexpected error occurred.';
        this.isLoading = false;
      }
    });
  }

  getSeverity(status: string): string {
    switch (status) {
      case 'UP':
        return 'success';
      case 'DOWN':
        return 'danger';
      case 'OUT_OF_SERVICE':
        return 'warning';
      default:
        return 'info';
    }
  }
}
