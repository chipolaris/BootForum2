import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../_services/admin.service';
import { MessageService, ConfirmationService } from 'primeng/api';
import { errorMessageFromApiResponse } from '../../_data/dtos';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

@Component({
  selector: 'app-admin-data',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmDialogModule],
  templateUrl: './admin-data.component.html',
  styleUrls: ['./admin-data.component.css'],
  providers: [ConfirmationService] // Provide the service for this component
})
export class AdminDataComponent {
  private adminService = inject(AdminService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  isUserLoading = false;
  isForumDataLoading = false;
  userCount = 50;

  constructor() {}

  generateUsers(): void {
    if (this.userCount <= 0 || this.userCount > 5000) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Invalid Input',
        detail: 'Please enter a number between 1 and 5,000.',
        life: 5000
      });
      return;
    }

    this.confirmationService.confirm({
      message: `Are you sure you want to generate ${this.userCount} new users? This may take some time.`,
      header: 'Confirm User Generation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.isUserLoading = true;
        this.adminService.triggerUserGeneration(this.userCount).subscribe({
          next: (response) => this.handleResponse(response, 'User generation process started.'),
          error: (err) => this.handleError(err),
          complete: () => this.isUserLoading = false
        });
      }
    });
  }

  generateForumData(): void {
    this.confirmationService.confirm({
      message: 'This is a one-time action to populate an empty database. Are you sure you want to proceed?',
      header: 'Confirm Forum Data Generation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.isForumDataLoading = true;
        this.adminService.triggerDataGeneration().subscribe({
          next: (response) => this.handleResponse(response, 'Forum data generation process started.'),
          error: (err) => this.handleError(err),
          complete: () => this.isForumDataLoading = false
        });
      }
    });
  }

  private handleResponse(response: any, summary: string): void {
    if (response.success) {
      this.messageService.add({
        severity: 'success',
        summary: 'Process Started',
        detail: response.data || 'Request accepted. Check server logs for progress.',
        life: 7000
      });
    } else {
      this.messageService.add({
        severity: 'error',
        summary: 'Error',
        detail: errorMessageFromApiResponse(response) || 'Failed to start the process.',
        life: 7000
      });
    }
  }

  private handleError(err: any): void {
    this.messageService.add({
      severity: 'error',
      summary: 'API Error',
      detail: err.message || 'An unexpected error occurred.',
      life: 7000
    });
    // Ensure loading spinners are turned off on error
    this.isUserLoading = false;
    this.isForumDataLoading = false;
  }
}
