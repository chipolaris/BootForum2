import { Component, OnInit, Input, Output, EventEmitter, inject, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CreateDiscussionPayload } from '../_data/dtos'; // Adjust path as needed

// Import Toast UI Editor specific modules
import Editor from '@toast-ui/editor'; // Import the type for the instance

@Component({
  selector: 'app-create-discussion',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './create-discussion.component.html',
  styleUrls: ['./create-discussion.component.css']
})
export class CreateDiscussionComponent implements OnInit, AfterViewInit, OnDestroy {

  @Input({ required: true }) forumId!: number;
  @Output() discussionSubmit = new EventEmitter<FormData>();

  // Get a reference to the Toast UI Editor component instance
  @ViewChild('commentEditorRef', { static: true }) commentEditorRef!: ElementRef;
  // Reference to the actual Toast UI Editor instance
  commentEditor: any | null = null;

  discussionForm!: FormGroup;
  selectedImages: FileList | null = null;
  selectedAttachments: FileList | null = null;
  submitted = false;
  isLoading = false;
  commentError: string | null = null; // Specific error for comment validation

  private fb = inject(FormBuilder);

  ngOnInit(): void {
    this.discussionForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(150)]],
      // Comment FormControl is still useful for tracking dirty/touched state,
      // but we'll get the value directly from the editor instance on submit.
      // We won't rely solely on its value for validation.
      comment: ['']
    });

    this.commentEditor = new Editor({
      el: this.commentEditorRef.nativeElement,
      height: '300px',
      initialEditType: 'markdown',
      previewStyle: 'vertical'
    });
  }

  ngAfterViewInit(): void {

/*     // Get the underlying Toast UI Editor instance after the view initializes
    if (this.toastEditorComponent) {
      this.toastEditorInstance = this.toastEditorComponent.getInstance();

      // Optional: Add event listeners if needed
      this.toastEditorInstance?.on('change', () => {
        // When editor content changes, clear the manual comment error
        // and potentially update the form control value if desired (though not strictly needed for submit)
        this.commentError = null;
        // this.discussionForm.controls['comment'].setValue(this.toastEditorInstance?.getMarkdown(), { emitEvent: false });
      });
    } else {
      console.error("Could not get Toast UI Editor instance.");
    }
   */
  }

  ngOnDestroy(): void {
     // Clean up editor instance if necessary, though Angular wrapper might handle it
     this.commentEditor?.destroy();
  }


  // Convenience getter for easy access to form fields in template
  get f() { return this.discussionForm.controls; }

  onImageChange(event: Event): void {
    const element = event.target as HTMLInputElement;
    this.selectedImages = element.files;
  }

  onAttachmentChange(event: Event): void {
    const element = event.target as HTMLInputElement;
    this.selectedAttachments = element.files;
  }

  onSubmit(): void {
    this.submitted = true;
    this.commentError = null; // Reset comment error

    // --- Manual Validation for Toast UI Editor Content ---
    const commentContent = this.commentEditor?.getMarkdown().trim() || '';
    if (!commentContent) {
        this.commentError = 'Comment content is required.';
        // Optionally mark the form control as invalid manually if needed elsewhere
        // this.f['comment'].setErrors({ required: true });
        return; // Stop submission if comment is empty
    }
    // --- End Manual Validation ---


    // Stop here if the rest of the form is invalid (e.g., title)
    if (this.discussionForm.invalid) {
      console.log('Form title is invalid');
      return;
    }

    this.isLoading = true;
    const formData = new FormData();

    // Append text data
    formData.append('forumId', this.forumId.toString());
    formData.append('title', this.f['title'].value);
    // Append comment content obtained directly from the editor instance
    formData.append('comment', commentContent);

    // Append images
    if (this.selectedImages) {
      for (let i = 0; i < this.selectedImages.length; i++) {
        formData.append('images', this.selectedImages[i], this.selectedImages[i].name);
      }
    }

    // Append attachments
    if (this.selectedAttachments) {
      for (let i = 0; i < this.selectedAttachments.length; i++) {
        formData.append('attachments', this.selectedAttachments[i], this.selectedAttachments[i].name);
      }
    }

    console.log('Submitting FormData (comment content retrieved from editor)');
    // Emit the FormData object
    this.discussionSubmit.emit(formData);

    // Parent component should handle resetting isLoading and potentially the form
  }

  // Optional: Method to reset the form
  resetForm(): void {
    this.submitted = false;
    this.discussionForm.reset();
    this.commentEditor?.setMarkdown(''); // Clear the editor
    this.selectedImages = null;
    this.selectedAttachments = null;
    this.commentError = null;
    // Manually clear file input elements if needed (using ViewChild or other methods)
  }
}
