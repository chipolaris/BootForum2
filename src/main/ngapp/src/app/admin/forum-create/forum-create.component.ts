import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router'; // Keep for fallback if not in dialog

import { ForumDTO, ForumCreateDTO, ApiResponse } from '../../_data/dtos';
import { IconPickerComponent, IconSelection } from '../../icon-picker/icon-picker.component';
import { ForumService } from '../../_services/forum.service';

// PrimeNG for dialogs and UI elements
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputSwitchModule } from 'primeng/inputswitch';
import { DynamicDialogRef, DynamicDialogConfig } from 'primeng/dynamicdialog';

@Component({
  selector: 'app-forum-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IconPickerComponent,
    ToastModule,            // For messages
    ProgressSpinnerModule,  // For loading
    ButtonModule,           // For pButton
    InputTextModule,        // For pInputText
    InputSwitchModule       // For p-inputSwitch for 'active'
  ],
  providers: [MessageService], // Provide if not already globally or by host
  templateUrl: './forum-create.component.html', // Ensure this template uses pButton etc.
  styleUrls: ['./forum-create.component.css']
})
export class ForumCreateComponent implements OnInit {

  forumForm!: FormGroup;
  submitted = false;
  isLoading = false;
  errorMessage: string | null = null;

  initialIconName: string | null = 'heroAcademicCapSolid';
  initialColor: string = '#4f46e5';
  initialActiveState: boolean = true;

  parentGroupId: number | null = null; // To store parent ID from dialog config

  private fb = inject(FormBuilder);
  private forumService = inject(ForumService);
  private router = inject(Router);
  private messageService = inject(MessageService);

  // Inject DialogRef and Config, make them optional for standalone use
  public dialogRef = inject(DynamicDialogRef, { optional: true });
  public config = inject(DynamicDialogConfig, { optional: true });

  ngOnInit(): void {
    if (this.config?.data?.parentGroupId) {
      this.parentGroupId = this.config.data.parentGroupId;
      console.log('ForumCreateComponent initialized with parentGroupId:', this.parentGroupId);
    }

    this.forumForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      icon: [this.initialIconName as string | null],
      // 'color' was used in forum-edit, let's be consistent or use 'iconColor' as in DTO
      iconColor: [this.initialColor, Validators.required], // Changed 'color' to 'iconColor' to match DTO
      active: [this.initialActiveState]
      // parentGroupId: [this.parentGroupId] // Can add to form if needed, or just use the class property
    });
  }

  get f() { return this.forumForm.controls; }

  handleIconSelection(selection: IconSelection): void {
    this.forumForm.patchValue({
      icon: selection.iconName,
      iconColor: selection.iconColor // Changed 'color' to 'iconColor'
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = null;

    if (this.forumForm.invalid) {
      this.messageService.add({severity: 'warn', summary: 'Validation Error', detail: 'Please check the form.'});
      Object.values(this.f).forEach(control => control.markAsTouched());
      return;
    }

    this.isLoading = true;

    const payload: ForumCreateDTO = {
      title: this.f['title'].value,
      description: this.f['description'].value,
      icon: this.f['icon'].value,
      iconColor: this.f['iconColor'].value, // Changed 'color' to 'iconColor'
      active: this.f['active'].value,
      parentGroupId: this.parentGroupId // Include parentGroupId
    };

    this.forumService.createForum(payload).subscribe({
      next: (response: ApiResponse<ForumDTO>) => {
        this.isLoading = false;
        if (response.success && response.data) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Forum created successfully!' });
          if (this.dialogRef) {
            this.dialogRef.close(response.data); // Close dialog with the created forum
          } else {
            this.router.navigate(['/app/admin/forums']); // Fallback navigation
          }
        } else {
          this.errorMessage = response.message || 'Failed to create forum.';
          this.messageService.add({ severity: 'error', summary: 'Creation Failed', detail: this.errorMessage });
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'An unexpected error occurred.';
        this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage || 'An unexpected error occurred.' });
      }
    });
  }

  resetForm(): void {
    // ... (implementation as before)
    this.submitted = false;
    this.errorMessage = null;
    this.forumForm.reset({
      icon: this.initialIconName,
      iconColor: this.initialColor, // Changed 'color' to 'iconColor'
      title: '',
      description: '',
      active: this.initialActiveState
    });
  }

  onCancel(): void {
    if (this.dialogRef) {
      this.dialogRef.close(); // Close dialog without data
    } else {
      this.router.navigate(['/app/admin/forums/all']); // Fallback navigation
    }
  }
}
