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
  isVoteDataLoading = false;
  userCount = 50;

  // Properties for discussion simulation config
  discussionConfig = {
    numberOfForumGroups: 2,
    minForumsPerGroup: 2,
    maxForumsPerGroup: 3,
    minDiscussionsPerForum: 3,
    maxDiscussionsPerForum: 5,
    minCommentsPerDiscussion: 10,
    maxCommentsPerDiscussion: 20
  };

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
      message: `Are you sure you want to simulate ${this.userCount} new users? This may take some time.`,
      header: 'Confirm User Generation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.isUserLoading = true;
        this.adminService.triggerUserSimulation(this.userCount).subscribe({
          next: (response) => this.handleResponse(response, 'User simulation process started.'),
          error: (err) => this.handleError(err),
          complete: () => this.isUserLoading = false
        });
      }
    });
  }

  generateForumData(): void {
    // Add validation for min/max values
    if (this.discussionConfig.minForumsPerGroup > this.discussionConfig.maxForumsPerGroup ||
      this.discussionConfig.minDiscussionsPerForum > this.discussionConfig.maxDiscussionsPerForum ||
      this.discussionConfig.minCommentsPerDiscussion > this.discussionConfig.maxCommentsPerDiscussion) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Invalid Input',
        detail: 'Min values cannot be greater than max values.',
        life: 5000
      });
      return;
    }

    this.confirmationService.confirm({
      message: 'This will create a new set of simulated forum content based on your parameters. Are you sure?',
      header: 'Confirm Forum Content Simulation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.isForumDataLoading = true;
        this.adminService.triggerDiscussionSimulation(this.discussionConfig).subscribe({
          next: (response) => this.handleResponse(response, 'Forum content simulation process started.'),
          error: (err) => this.handleError(err),
          complete: () => this.isForumDataLoading = false
        });
      }
    });
  }

  /**
   * Triggers the generation of simulated votes.
   */
  generateVotes(): void {
    this.confirmationService.confirm({
      message: 'This will generate a large number of simulated up/down votes for existing content. Are you sure you want to proceed?',
      header: 'Confirm Vote Simulation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.isVoteDataLoading = true;
        this.adminService.triggerVoteSimulation().subscribe({
          next: (response) => this.handleResponse(response, 'Vote simulation process started.'),
          error: (err) => this.handleError(err),
          complete: () => this.isVoteDataLoading = false
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
    this.isVoteDataLoading = false;
  }
}
