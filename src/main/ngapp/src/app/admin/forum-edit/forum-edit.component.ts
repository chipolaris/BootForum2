import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router'; // Import ActivatedRoute

import { ForumDTO, ForumUpdateDTO, ApiResponse } from '../../_data/dtos';
import { IconPickerComponent, IconSelection } from '../../icon-picker/icon-picker.component';
import { ForumService } from '../../_services/forum.service';
import { MessageService } from 'primeng/api'; // For Toast messages
import { ToastModule } from 'primeng/toast';   // For Toast messages
import { ProgressSpinnerModule } from 'primeng/progressspinner'; // For loading indicator

@Component({
  selector: 'app-edit-forum',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IconPickerComponent,
    ToastModule,
    ProgressSpinnerModule
  ],
  providers: [MessageService], // Provide MessageService for PrimeNG Toast
  templateUrl: './forum-edit.component.html',
  styleUrls: ['./forum-edit.component.css'] // You can reuse create-forum.component.css or create a new one
})
export class ForumEditComponent implements OnInit {
  forumForm!: FormGroup;
  submitted = false;
  isLoading = false; // For general loading state (e.g., submission)
  isFetching = false; // For initial data fetching
  errorMessage: string | null = null;
  forumId: number | null = null;

  // Initial values for the form, will be overridden by fetched data
  initialIconName: string | null = 'heroArchiveBoxArrowDown';
  initialColor: string | null = '#4f46e5';
  initialActiveState: boolean = true;

  private fb = inject(FormBuilder);
  private forumService = inject(ForumService);
  private router = inject(Router);
  private route = inject(ActivatedRoute); // To get route params
  private messageService = inject(MessageService);

  ngOnInit(): void {
    this.forumForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      icon: [this.initialIconName as string | null], // Will be icon name
      color: [this.initialColor, Validators.required], // Will be icon hex color
      active: [this.initialActiveState]
    });

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.forumId = +id; // Convert string to number
        this.loadForumData(this.forumId);
      } else {
        this.errorMessage = 'Forum ID not found in route.';
        this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage });
        // Optionally navigate away if no ID
        // this.router.navigate(['/app/dashboard']);
      }
    });
  }

  loadForumData(id: number): void {
    this.isFetching = true;
    this.errorMessage = null;
    this.forumService.getForumById(id).subscribe({
      next: (apiResponse: ApiResponse<ForumDTO>) => {
        if (apiResponse.success && apiResponse.data) {
          const forumData = apiResponse.data;
          this.forumForm.patchValue({
            title: forumData.title,
            description: forumData.description,
            icon: forumData.icon,           // Forum.icon is the icon name
            color: forumData.iconColor,     // Forum.iconColor is the hex color
            active: forumData.active
          });
          // Update initial values for icon picker if needed
          this.initialIconName = forumData.icon;
          this.initialColor = forumData.iconColor;
          this.initialActiveState = forumData.active;
        } else {
          this.errorMessage = apiResponse.message || 'Failed to load forum data.';
          this.messageService.add({ severity: 'error', summary: 'Load Failed', detail: this.errorMessage });
        }
        this.isFetching = false;
      },
      error: (err) => {
        this.isFetching = false;
        this.errorMessage = err.message || 'An error occurred while fetching forum data.';
        this.messageService.add({ severity: 'error', summary: 'Load Error', detail: this.errorMessage || " Error " });
      }
    });
  }

  get f() { return this.forumForm.controls; }

  handleIconSelection(selection: IconSelection): void {
    this.forumForm.patchValue({
      icon: selection.iconName,
      color: selection.iconColor
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = null;

    if (this.forumForm.invalid) {
      this.messageService.add({ severity: 'warn', summary: 'Validation Error', detail: 'Please correct the form errors.' });
      console.log('Form is invalid:', this.forumForm.errors);
      return;
    }

    if (this.forumId === null) {
      this.errorMessage = 'Forum ID is missing. Cannot update.';
      this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage });
      return;
    }

    this.isLoading = true;

    // Construct payload based on Angular's ForumDTO (dtos.ts)
    // This assumes your Java backend can map these fields correctly
    // (e.g., Angular 'icon' to Java 'iconName' field, Angular 'iconColor' to Java 'color' field if names differ)
    // Based on your CreateForumComponent, your Angular DTO uses 'icon' for name and 'iconColor' for hex.
    const payload: ForumUpdateDTO = {
      id: this.forumId,
      title: this.f['title'].value,
      description: this.f['description'].value,
      icon: this.f['icon'].value,          // Icon Name
      iconColor: this.f['color'].value,    // Icon Hex Color
      active: this.f['active'].value,
    };

    console.log('Submitting Updated Forum Payload to service:', payload);

    this.forumService.updateForum(this.forumId, payload).subscribe({
      next: (apiResponse: ApiResponse<ForumDTO>) => {
        this.isLoading = false;
        if (apiResponse.success && apiResponse.data) {
          console.log('Forum update successful', apiResponse.data);
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Forum updated successfully!' });
          // Navigate to a different page, e.g., dashboard or forum list
          this.router.navigate(['/app/admin/forums']);
        } else {
          this.errorMessage = apiResponse.message || 'Failed to update forum.';
          this.messageService.add({ severity: 'error', summary: 'Update Failed', detail: this.errorMessage || " Error " });
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'An error occurred during forum update.';
        this.messageService.add({ severity: 'error', summary: 'Update Error', detail: this.errorMessage || " Error " });
        console.error('Forum update failed', err);
      }
    });
  }

  // Optional: A cancel button could navigate back
  onCancel(): void {
    // Navigate back or to a default page
    this.router.navigate(['/app/admin/forums']); // Or use Location.back()
  }
}
