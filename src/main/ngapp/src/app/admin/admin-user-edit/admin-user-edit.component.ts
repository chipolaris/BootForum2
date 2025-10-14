import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef, DialogService } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { AdminUserService } from '../../_services/admin-user.service';
import { AdminUserUpdateDTO, UserSummaryDTO, errorMessageFromApiResponse } from '../../_data/dtos';
import { MultiSelectModule } from 'primeng/multiselect';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { AdminPasswordChangeComponent } from '../admin-password-change/admin-password-change.component';

@Component({
  selector: 'app-admin-user-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MultiSelectModule, DropdownModule, ButtonModule],
  providers: [DialogService],
  templateUrl: './admin-user-edit.component.html',
})
export class AdminUserEditComponent implements OnInit {
  private fb = inject(FormBuilder);
  private config = inject(DynamicDialogConfig);
  private ref = inject(DynamicDialogRef);
  private adminUserService = inject(AdminUserService);
  private messageService = inject(MessageService);
  private dialogService = inject(DialogService);

  user!: UserSummaryDTO;
  editForm!: FormGroup;
  isSubmitting = false;

  availableRoles = [{ label: 'Admin', value: 'ADMIN' }, { label: 'User', value: 'USER' }];
  accountStatuses = [{ label: 'Active', value: 'ACTIVE' }, { label: 'Locked', value: 'LOCKED' }, { label: 'Inactive', value: 'INACTIVE' }];

  ngOnInit(): void {
    this.user = this.config.data.user;
    this.editForm = this.fb.group({
      roles: [this.user.roles],
      accountStatus: [this.user.accountStatus]
    });
  }

  onSubmit(): void {
    if (this.editForm.invalid) {
      return;
    }
    this.isSubmitting = true;
    const payload: AdminUserUpdateDTO = this.editForm.value;

    this.adminUserService.updateUser(this.user.id, payload).subscribe({
      next: response => {
        if (response.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'User updated successfully.' });
          this.ref.close(true); // Close dialog and signal success
        } else {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessageFromApiResponse(response) || 'Failed to update user.' });
        }
        this.isSubmitting = false;
      },
      error: err => {
        this.isSubmitting = false;
        this.messageService.add({ severity: 'error', summary: 'Server Error', detail: err.message || 'An unexpected error occurred.' });
      }
    });
  }

  openPasswordChangeDialog(): void {
    this.dialogService.open(AdminPasswordChangeComponent, {
      header: `Change Password for ${this.user.username}`,
      width: 'min(90%, 400px)',
      modal: true,
      data: { userId: this.user.id }
    });
  }

  onCancel(): void {
    this.ref.close();
  }
}
