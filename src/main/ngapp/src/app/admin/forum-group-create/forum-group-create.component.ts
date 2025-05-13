import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ForumGroupDTO } from '../../_data/dtos';
import { IconPickerComponent, IconSelection } from '../../icon-picker/icon-picker.component';
import { ForumGroupService } from '../../_services/forum-group.service';

// PrimeNG for messages and spinner
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ButtonModule } from 'primeng/button'; // For pButton directive
import { InputTextModule } from 'primeng/inputtext'; // For pInputText directive

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
  providers: [MessageService], // Provide MessageService for p-toast
  templateUrl: './forum-group-create.component.html',
  styleUrls: ['./forum-group-create.component.css']
})
export class ForumGroupCreateComponent implements OnInit {

  forumGroupForm!: FormGroup;
  submitted = false;
  isLoading = false;
  errorMessage: string | null = null;

  // Default values similar to ForumCreateComponent
  initialIconName: string | null = 'heroAcademicCap'; // Different default icon
  initialColor: string = '#3b82f6'; // A different default color (e.g., blue)

  private fb = inject(FormBuilder);
  private forumGroupService = inject(ForumGroupService);
  private router = inject(Router);
  private messageService = inject(MessageService);

  ngOnInit(): void {
    this.forumGroupForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(100)]],
      icon: [this.initialIconName as string | null], // No required validator, can be null
      iconColor: [this.initialColor, Validators.required] // Color is usually tied to icon
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
      console.log('Form is invalid:', this.forumGroupForm.errors);
      this.messageService.add({ severity: 'warn', summary: 'Validation Error', detail: 'Please check the form for errors.' });
      // Mark all fields as touched to display errors
      Object.values(this.f).forEach(control => {
        control.markAsTouched();
      });
      return;
    }

    this.isLoading = true;

    const payload: ForumGroupDTO = {
      title: this.f['title'].value,
      icon: this.f['icon'].value,
      iconColor: this.f['iconColor'].value,
    };

    this.forumGroupService.createForumGroup(payload).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Forum Group created successfully!' });
          this.resetForm();
          // Navigate to a list of forum groups or admin dashboard
          this.router.navigate(['/app/dashboard']); // Or a future '/app/admin/forum-groups'
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
    this.router.navigate(['/app/admin/dashboard']); // Or wherever appropriate
  }
}
