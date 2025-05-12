import { Component, OnInit, inject } from '@angular/core'; // Removed EventEmitter, Output
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router'; // Import Router

import { ForumDTO } from '../../_data/dtos';
import { IconPickerComponent, IconSelection } from '../../icon-picker/icon-picker.component';
import { ForumService } from '../../_services/forum.service'; // Import your ForumService (adjust path)
// Optional: For displaying success/error messages
// import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-forum-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    IconPickerComponent
  ],
  templateUrl: './forum-create.component.html',
  styleUrls: ['./forum-create.component.css']
})
export class ForumCreateComponent implements OnInit {

  forumForm!: FormGroup;
  submitted = false;
  isLoading = false;
  errorMessage: string | null = null; // For displaying errors

  initialIconName: string | null = 'heroArchiveBoxArrowDown';
  initialColor: string = '#4f46e5';
  initialActiveState: boolean = true;

  private fb = inject(FormBuilder);
  private forumService = inject(ForumService); // INJECTED
  private router = inject(Router);           // INJECTED
  // private toastr = inject(ToastrService); // Optional: for notifications

  ngOnInit(): void {
    this.forumForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.maxLength(500)]],
      icon: [this.initialIconName as string | null],
      color: [this.initialColor, Validators.required],
      active: [this.initialActiveState]
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
    this.errorMessage = null; // Clear previous errors

    if (this.forumForm.invalid) {
      console.log('Form is invalid:', this.forumForm.errors);
      return;
    }

    this.isLoading = true;

    const payload: ForumDTO = {
      title: this.f['title'].value,
      description: this.f['description'].value,
      icon: this.f['icon'].value,
      iconColor: this.f['color'].value,
      active: this.f['active'].value,
    };

    console.log('Submitting Forum Payload directly to service:', payload);

    // Call the service instead of emitting
    this.forumService.createForum(payload).subscribe({
      next: (response) => {
        this.isLoading = false;
        console.log('Forum creation successful', response);
        // this.toastr.success('Forum created successfully!'); // Optional
        this.resetForm();
        // Navigate to a different page, e.g., the new forum's page or a list
        // If the response contains the new forum's ID or slug:
        // this.router.navigate(['/forums', response.id]);
        this.router.navigate(['/app/forum-list']);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.message || 'Failed to create forum. Please try again.';
        console.error('Forum creation failed', err);
        // this.toastr.error(this.errorMessage); // Optional
      }
    });
  }

  resetForm(): void {
    this.submitted = false;
    this.errorMessage = null;
    this.forumForm.reset({
      icon: this.initialIconName,
      color: this.initialColor,
      title: '',
      description: '',
      active: this.initialActiveState
    });
  }

  // Optional: A cancel button could navigate back
  onCancel(): void {
    // Navigate back or to a default page
    this.router.navigate(['/app/admin/forums']); // Or use Location.back()
  }

}
