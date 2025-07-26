import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router'; // Keep for fallback

import { ForumGroupDTO, ForumGroupCreateDTO, ApiResponse } from '../../_data/dtos'; // Ensure ApiResponse is imported
import { IconPickerComponent, IconSelection } from '../../icon-picker/icon-picker.component';
import { ForumGroupService } from '../../_services/forum-group.service';

// PrimeNG for messages, spinner, and dialogs
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog'; // <<< ADDED

@Component({
  selector: 'app-forum-group-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IconPickerComponent,
    ToastModule,
    ProgressSpinnerModule,
    ButtonModule,
    InputTextModule
  ],
  providers: [MessageService],
  templateUrl: './forum-group-create.component.html',
  styleUrls: ['./forum-group-create.component.css']
})
export class ForumGroupCreateComponent implements OnInit {

  forumGroupForm!: FormGroup;
  submitted = false;
  isLoading = false;
  errorMessage: string | null = null;

  initialIconName: string | null = 'heroAcademicCapSolid';
  initialColor: string = '#3b82f6';

  parentGroupId: number | null = null; // <<< ADDED: To store parent ID

  private fb = inject(FormBuilder);
  private forumGroupService = inject(ForumGroupService);
  private router = inject(Router);
  private messageService = inject(MessageService);

  // <<< ADDED: Inject DialogRef and Config, make them optional for standalone use
  public dialogRef = inject(DynamicDialogRef, { optional: true });
  public config = inject(DynamicDialogConfig, { optional: true });

  ngOnInit(): void {
    // <<< ADDED: Retrieve parentGroupId from dialog config
    if (this.config?.data?.parentGroupId) {
      this.parentGroupId = this.config.data.parentGroupId;
      console.log('ForumGroupCreateComponent initialized with parentGroupId:', this.parentGroupId);
    }

    this.forumGroupForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(100)]],
      icon: [this.initialIconName as string | null],
      iconColor: [this.initialColor, Validators.required]
    });
  }

  get f() { return this.forumGroupForm.controls; }

  handleIconSelection(selection: IconSelection): void {
    this.forumGroupForm.patchValue({
      icon: selection.iconName,
      iconColor: selection.iconColor
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = null;

    if (this.forumGroupForm.invalid) {
      this.messageService.add({ severity: 'warn', summary: 'Validation Error', detail: 'Please check the form for errors.' });
      Object.values(this.f).forEach(control => {
        control.markAsTouched();
      });
      return;
    }

    this.isLoading = true;

    const payload: ForumGroupCreateDTO = {
      title: this.f['title'].value,
      icon: this.f['icon'].value,
      iconColor: this.f['iconColor'].value,
      parentGroupId: this.parentGroupId // <<< ADDED: Include parentGroupId
    };

    this.forumGroupService.createForumGroup(payload).subscribe({
      next: (response: ApiResponse<ForumGroupDTO>) => { // <<< Ensure response type is ApiResponse<ForumGroupDTO>
        this.isLoading = false;
        if (response.success && response.data) { // <<< Check response.data
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Forum Group created successfully!' });
          if (this.dialogRef) {
            this.dialogRef.close(response.data); // <<< ADDED: Close dialog with created group
          } else {
            this.resetForm(); // Reset form if not in dialog
            this.router.navigate(['/app/admin/dashboard']); // Fallback navigation
          }
        } else {
          this.errorMessage = response.message || 'Failed to create forum group.';
          this.messageService.add({ severity: 'error', summary: 'Creation Failed', detail: this.errorMessage });
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'An unexpected error occurred. Please try again.';
        this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage || 'An unexpected error occurred.' });
        console.error('Forum Group creation failed', err);
      }
    });
  }

  resetForm(): void {
    this.submitted = false;
    this.errorMessage = null;
    this.forumGroupForm.reset({
      title: '',
      icon: this.initialIconName,
      iconColor: this.initialColor,
    });
  }

  onCancel(): void {
    // <<< ADDED: Handle dialog close on cancel
    if (this.dialogRef) {
      this.dialogRef.close();
    } else {
      this.router.navigate(['/app/admin/forums/all']);
    }
  }
}
