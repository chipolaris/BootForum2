// src/app/admin/forum-group-edit/forum-group-edit.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router'; // ActivatedRoute to get ID
import { switchMap } from 'rxjs/operators';

import { ForumGroupDTO, ForumGroupUpdateDTO } from '../../_data/dtos';
import { IconPickerComponent, IconSelection } from '../../icon-picker/icon-picker.component';
import { ForumGroupService } from '../../_services/forum-group.service';

// PrimeNG
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

@Component({
  selector: 'app-forum-group-edit',
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
  templateUrl: './forum-group-edit.component.html',
  styleUrls: ['./forum-group-edit.component.css']
})
export class ForumGroupEditComponent implements OnInit {

  forumGroupForm!: FormGroup;
  submitted = false;
  isLoading = false; // For general page loading/submission
  isFetching = true; // Specifically for initial data fetch
  errorMessage: string | null = null;
  forumGroupId: number | null = null;

  // These will be set from fetched data
  currentIconName: string | null = null;
  currentIconColor: string = '#333333'; // Default fallback for icon picker

  private fb = inject(FormBuilder);
  private forumGroupService = inject(ForumGroupService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private messageService = inject(MessageService);

  ngOnInit(): void {
    this.forumGroupForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(100)]],
      icon: [null as string | null],
      iconColor: [this.currentIconColor, Validators.required]
    });

    this.route.paramMap.pipe(
      switchMap(params => {
        const id = params.get('id');
        if (id) {
          this.forumGroupId = +id;
          this.isFetching = true;
          return this.forumGroupService.getForumGroupById(this.forumGroupId);
        } else {
          this.errorMessage = 'Forum Group ID not found in URL.';
          this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage });
          this.isFetching = false;
          this.router.navigate(['/app/dashboard']); // Or a list page
          throw new Error(this.errorMessage);
        }
      })
    ).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.populateForm(response.data);
        } else {
          this.errorMessage = response.message || 'Failed to load forum group data.';
          this.messageService.add({ severity: 'error', summary: 'Load Failed', detail: this.errorMessage });
        }
        this.isFetching = false;
      },
      error: (err) => {
        this.errorMessage = err.message || 'An unexpected error occurred while fetching data.';
        this.messageService.add({ severity: 'error', summary: 'Fetch Error', detail: this.errorMessage || 'An unexpected error occurred.' });
        this.isFetching = false;
      }
    });
  }

  populateForm(data: ForumGroupDTO): void {
    this.forumGroupForm.patchValue({
      title: data.title,
      icon: data.icon,
      iconColor: data.iconColor ?? this.currentIconColor // Use fetched or fallback
    });
    this.currentIconName = data.icon;
    this.currentIconColor = data.iconColor ?? this.currentIconColor; // Ensure icon picker gets a string
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
      Object.values(this.f).forEach(control => control.markAsTouched());
      return;
    }

    if (!this.forumGroupId) {
      this.errorMessage = "Forum Group ID is missing. Cannot update.";
      this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage || 'An unexpected error occurred.' });
      return;
    }

    this.isLoading = true;

    const payload: ForumGroupUpdateDTO = {
      id: this.forumGroupId, // ID is in URL, not typically in PUT payload body unless API requires
      title: this.f['title'].value,
      icon: this.f['icon'].value,
      iconColor: this.f['iconColor'].value,
    };

    this.forumGroupService.updateForumGroup(this.forumGroupId, payload).subscribe({
      next: (response) => {
        this.isLoading = false;
        if (response.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Forum Group updated successfully!' });
          // Optionally navigate away, e.g., to a list or dashboard
          this.router.navigate(['/app/dashboard']); // Or a future '/app/admin/forum-groups'
        } else {
          this.errorMessage = response.message || 'Failed to update forum group.';
          this.messageService.add({ severity: 'error', summary: 'Update Failed', detail: this.errorMessage || 'An unexpected error occurred.' });
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'An unexpected error occurred. Please try again.';
        this.messageService.add({ severity: 'error', summary: 'Error', detail: this.errorMessage || 'An unexpected error occurred.' });
      }
    });
  }

  onCancel(): void {
    // Navigate back or to a default admin page
    this.router.navigate(['/app/dashboard']); // Or use Location.back() or to a list page
  }
}
