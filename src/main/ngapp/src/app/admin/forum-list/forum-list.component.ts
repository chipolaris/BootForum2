import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router'; // RouterModule for routerLink

import { Forum } from '../../_data/models';
import { ApiResponse } from '../../_data/dtos';
import { ForumService } from '../../_services/forum.service';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag'; // For 'Active' status
import { TooltipModule } from 'primeng/tooltip'; // For tooltips on icons/buttons
import { ProgressSpinnerModule } from 'primeng/progressspinner';

@Component({
  selector: 'app-forum-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule, // For routerLink
    ToastModule,
    TableModule,
    ButtonModule,
    TagModule,
    TooltipModule,
    ProgressSpinnerModule
  ],
  providers: [MessageService],
  templateUrl: './forum-list.component.html',
  styleUrls: ['./forum-list.component.css']
})
export class ForumListComponent implements OnInit {
  forums: Forum[] = [];
  isLoading = false;
  errorMessage: string | null = null;

  private forumService = inject(ForumService);
  private messageService = inject(MessageService);
  private router = inject(Router);

  ngOnInit(): void {
    this.loadForums();
  }

  loadForums(): void {
    this.isLoading = true;
    this.errorMessage = null;
    this.forumService.getAllForums().subscribe({
      next: (apiResponse: ApiResponse<Forum[]>) => {
        if (apiResponse.success && apiResponse.data) {
          this.forums = apiResponse.data;
        } else {
          this.errorMessage = apiResponse.message || 'Failed to load forums.';
          this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage });
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'An unexpected error occurred.';
        this.messageService.add({ severity: 'error', summary: 'Fetch Error', detail: this.errorMessage || "Unable to load forums"});
      }
    });
  }

  editForum(forumId: number | undefined): void {
    if (forumId === undefined) {
      console.error("Cannot edit forum with undefined ID");
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Forum ID is missing.' });
      return;
    }
    this.router.navigate(['/app/admin/forums', forumId]);
  }

  // Placeholder for delete functionality
  deleteForum(forumId: number | undefined): void {
    if (forumId === undefined) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Forum ID is missing for deletion.' });
      return;
    }
    this.messageService.add({
      severity: 'info',
      summary: 'Info',
      detail: `Delete functionality for forum ID ${forumId} is not yet implemented.`
    });
    console.log('Attempting to delete forum (not implemented):', forumId);
    // Implement actual deletion logic here, perhaps with a confirmation dialog
    // e.g., call a service method, then reload forums or remove from list
  }

  getSeverity(active: boolean): "success" | "danger" {
    return active ? 'success' : 'danger';
  }
}
