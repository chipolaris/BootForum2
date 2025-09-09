import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { AdminUserService } from '../../_services/admin-user.service';
import { AdminPasswordChangeDTO, errorMessageFromApiResponse } from '../../_data/dtos';
import { ButtonModule } from 'primeng/button';
import { PasswordModule } from 'primeng/password';

@Component({
  selector: 'app-admin-password-change',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonModule, PasswordModule],
  template: `
    <form [formGroup]="passwordForm" (ngSubmit)="onSubmit()" class="p-fluid">
      <div class="field">
        <label for="newPassword">New Password</label>
        <p-password id="newPassword" formControlName="newPassword" [toggleMask]="true" [feedback]="true"></p-password>
      </div>
      <div class="flex justify-end gap-2 mt-5">
        <button pButton type="button" label="Cancel" (click)="onCancel()" class="p-button-text"></button>
        <button pButton type="submit" label="Set Password" [loading]="isSubmitting" [disabled]="passwordForm.invalid"></button>
      </div>
    </form>
  `
})
export class AdminPasswordChangeComponent implements OnInit {
  private fb = inject(FormBuilder);
  private config = inject(DynamicDialogConfig);
  private ref = inject(DynamicDialogRef);
  private adminUserService = inject(AdminUserService);
  private messageService = inject(MessageService);

  userId!: number;
  passwordForm!: FormGroup;
  isSubmitting = false;

  ngOnInit(): void {
    this.userId = this.config.data.userId;
    this.passwordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onSubmit(): void {
    if (this.passwordForm.invalid) return;
    this.isSubmitting = true;
    const payload: AdminPasswordChangeDTO = this.passwordForm.value;

    this.adminUserService.changePassword(this.userId, payload).subscribe({
      next: response => {
        if (response.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Password changed successfully.' });
          this.ref.close(true);
        } else {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessageFromApiResponse(response) || 'Failed to change password.' });
        }
        this.isSubmitting = false;
      },
      error: err => {
        this.isSubmitting = false;
        this.messageService.add({ severity: 'error', summary: 'Server Error', detail: err.message || 'An unexpected error occurred.' });
      }
    });
  }

  onCancel(): void {
    this.ref.close();
  }
}
