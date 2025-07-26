import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../_services/admin.service';
import { MessageService } from 'primeng/api';
import { errorMessageFromApiResponse } from '../../_data/dtos';

@Component({
  selector: 'app-admin-index',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-index.component.html',
})
export class AdminIndexComponent {
  private adminService = inject(AdminService);
  private messageService = inject(MessageService);

  isLoading = false;
  reindexTarget: 'all' | 'Discussion' | 'Comment' = 'all';

  constructor() {}

  startReindexing(): void {
    this.isLoading = true;

    this.adminService.triggerReindex(this.reindexTarget).subscribe({
      next: (response) => {
        if (response.success) {
          this.messageService.add({
            severity: 'success', // This ensures the green 'success' toast is shown
            summary: 'Process Started',
            detail: response.data || 'Request accepted. Check server logs for progress.', // Use data, with a fallback.
            life: 7000 // Show message for 7 seconds
          });
        } else {
          const errorMessage = errorMessageFromApiResponse(response);
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: errorMessage || 'Failed to start re-indexing process.',
            life: 7000
          });
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.messageService.add({
          severity: 'error',
          summary: 'API Error',
          detail: err.message || 'An unexpected error occurred. Check browser console for details.',
          life: 7000
        });
        this.isLoading = false;
      }
    });
  }
}
