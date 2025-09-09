import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminUserService } from '../../_services/admin-user.service';
import { UserSummaryDTO, errorMessageFromApiResponse } from '../../_data/dtos';
import { MessageService } from 'primeng/api';
// FIX: Import the specific TableLazyLoadEvent from primeng/table
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';
import { ToastModule } from 'primeng/toast';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { AdminUserEditComponent } from '../admin-user-edit/admin-user-edit.component';

@Component({
  selector: 'app-admin-user-list',
  standalone: true,
  imports: [CommonModule, TableModule, TagModule, ButtonModule, TooltipModule, ToastModule, DynamicDialogModule, DatePipe],
  providers: [DialogService, MessageService],
  templateUrl: './admin-user-list.component.html',
  styleUrls: ['./admin-user-list.component.css']
})
export class AdminUserListComponent implements OnInit {
  private adminUserService = inject(AdminUserService);
  private messageService = inject(MessageService);
  private dialogService = inject(DialogService);

  users: UserSummaryDTO[] = [];
  totalRecords = 0;
  loading = true;

  ngOnInit(): void { }

  // FIX: Update the method to accept the specific event type from the table
  loadUsers(event: TableLazyLoadEvent): void {
    this.loading = true;
    this.adminUserService.getUsers(event).subscribe({
      next: response => {
        if (response.success && response.data) {
          this.users = response.data.content;
          this.totalRecords = response.data.totalElements;
        } else {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessageFromApiResponse(response) || 'Failed to load users.' });
        }
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.messageService.add({ severity: 'error', summary: 'Server Error', detail: err.message || 'An unexpected error occurred.' });
      }
    });
  }

  openEditDialog(user: UserSummaryDTO): void {
    const ref = this.dialogService.open(AdminUserEditComponent, {
      header: `Edit User: ${user.username}`,
      width: 'min(90%, 500px)',
      data: { user }
    });

    ref.onClose.subscribe((result: boolean) => {
      if (result) {
        // FIX: Update the manual event to also use the more specific type for consistency
        const event: TableLazyLoadEvent = { first: 0, rows: 10 };
        this.loadUsers(event);
      }
    });
  }

  getStatusSeverity(status: string): "success" | "warning" | "danger" {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'LOCKED': return 'danger';
      case 'INACTIVE': return 'warning';
      default: return 'warning';
    }
  }
}
