import { Component, OnInit, Output, EventEmitter, inject, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

// Import Toast UI Editor specific modules
import Editor from '@toast-ui/editor';
import { DiscussionService } from '../_services/discussion.service';
import { ForumService } from '../_services/forum.service';
import { DiscussionDTO } from '../_data/dtos'; // Assuming ForumDTO is not directly used here or part of DiscussionDTO
import { finalize } from 'rxjs/operators';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-discussion-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './discussion-create.component.html',
  styleUrls: ['./discussion-create.component.css']
})
export class DiscussionCreateComponent implements OnInit, OnDestroy {

  forumId!: number;
  forumTitle: string | null = null;
  @Output() discussionCreationResult = new EventEmitter<{ success: boolean, data?: DiscussionDTO | null, error?: string }>();

  contentEditor: InstanceType<typeof Editor> | null = null;

  // Use a setter for ViewChild to react when the element is available
  @ViewChild('contentEditorRef') private set editorContentEl(el: ElementRef | undefined) {
    if (el && el.nativeElement && !this.contentEditor) {
      // Element is now available, and editor not yet initialized
      this.initializeEditor(el.nativeElement);
    }
  }

  discussionForm!: FormGroup;
  selectedImages: FileList | null = null;
  selectedAttachments: FileList | null = null;
  submitted = false;
  isLoading = true;
  contentError: string | null = null;
  generalError: string | null = null;

  private fb = inject(FormBuilder);
  private discussionService = inject(DiscussionService);
  private forumService = inject(ForumService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private routeSubscription: Subscription | undefined;
  private forumTitleSubscription: Subscription | undefined;

  ngOnInit(): void {
    this.routeSubscription = this.route.paramMap.subscribe(params => {
      const idParam = params.get('forumId');
      if (idParam) {
        const parsedId = +idParam;
        if (!isNaN(parsedId)) {
          this.forumId = parsedId;
          this.fetchForumTitleAndPrepareForm();
        } else {
          this.isLoading = false;
          this.generalError = 'Invalid Forum ID provided in the URL.';
          console.error(this.generalError, 'Received:', idParam);
        }
      } else {
        this.isLoading = false;
        this.generalError = 'Forum ID not found in the URL. Cannot create discussion.';
        console.error(this.generalError);
      }
    });
  }

  private fetchForumTitleAndPrepareForm(): void {
    this.isLoading = true;
    this.forumTitleSubscription = this.forumService.getForumById(this.forumId).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.forumTitle = response.data.title;
        } else {
          console.warn('Could not fetch forum title or forum data is incomplete.');
        }
        this.setupForm(); // Setup form and trigger rendering
      },
      error: (err) => {
        console.error('Error fetching forum details for title:', err);
        this.generalError = 'Failed to load forum details. You can still create a discussion.';
        this.setupForm(); // Still setup form even if title fails
      }
    });
  }

  private setupForm(): void {
    // 1. Initialize the form data structure.
    // This makes `discussionForm` truthy for the *ngIf in the template.
    this.discussionForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(150)]],
      content: [''] // Content will be from the editor instance
    });

    // 2. Set isLoading to false.
    // This, combined with `discussionForm` being truthy, should trigger Angular
    // to render the form and its contents. If #contentEditorRef is rendered,
    // the @ViewChild setter `editorContentEl` will be called.
    this.isLoading = false;
  }

  private initializeEditor(element: HTMLElement): void {
    // This method is now called by the @ViewChild setter when the element is ready.
    if (this.contentEditor) { // Prevent re-initialization
      console.warn('Editor already initialized. Skipping re-initialization.');
      return;
    }
    try {
      this.contentEditor = new Editor({
        el: element,
        height: '300px',
        initialEditType: 'markdown',
        previewStyle: 'vertical'
      });

      this.contentEditor.on('change', () => {
        if (this.contentError && this.contentEditor?.getMarkdown().trim()) {
          this.contentError = null;
        }
      });
      console.log('Toast UI Editor initialized successfully via ViewChild setter.');
    } catch (e) {
      console.error('Error during Editor instantiation:', e);
      this.generalError = 'Failed to initialize the text editor.';
    }
  }

  ngOnDestroy(): void {
     this.contentEditor?.destroy();
     this.routeSubscription?.unsubscribe();
     this.forumTitleSubscription?.unsubscribe();
  }

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
    if (this.forumId === undefined || this.forumId === null || isNaN(this.forumId)) {
      this.generalError = 'Cannot create discussion: Forum ID is missing or invalid.';
      console.error(this.generalError, 'Current forumId:', this.forumId);
      this.isLoading = false;
      return;
    }

    this.submitted = true;
    this.contentError = null;
    this.generalError = null;

    const discussionContent = this.contentEditor?.getMarkdown().trim() || '';
    if (!discussionContent) {
        this.contentError = 'Discussion content is required.';
        return;
    }

    if (this.discussionForm.invalid) {
      console.log('Form title is invalid');
      Object.values(this.discussionForm.controls).forEach(control => {
        control.markAsTouched();
      });
      return;
    }

    this.isLoading = true;
    const formData = new FormData();

    formData.append('forumId', this.forumId.toString());
    formData.append('title', this.f['title'].value);
    formData.append('content', discussionContent);

    if (this.selectedImages) {
      for (let i = 0; i < this.selectedImages.length; i++) {
        formData.append('images', this.selectedImages[i], this.selectedImages[i].name);
      }
    }

    if (this.selectedAttachments) {
      for (let i = 0; i < this.selectedAttachments.length; i++) {
        formData.append('attachments', this.selectedAttachments[i], this.selectedAttachments[i].name);
      }
    }

    this.discussionService.createDiscussion(formData)
      .pipe(
        finalize(() => this.isLoading = false)
      )
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.discussionCreationResult.emit({ success: true, data: response.data });
            // Navigate after successful creation
            if (response.data.id) { // Assuming DiscussionDTO has an id
                this.router.navigate(['/app/forums', this.forumId, 'view']);
            } else {
                this.router.navigate(['/app/forums', this.forumId, 'view']); // Fallback
            }
            this.resetForm();
          } else {
            this.generalError = response.message || 'Failed to create discussion. Please try again.';
            if(response.errors && response.errors.length > 0) {
                this.generalError += ` Details: ${response.errors.join(', ')}`;
            }
            this.discussionCreationResult.emit({ success: false, error: this.generalError ?? undefined});
          }
        },
        error: (err) => {
          this.generalError = err.message || 'An unexpected error occurred. Please try again.';
          this.discussionCreationResult.emit({ success: false, error: this.generalError ?? undefined});
        }
      });
  }

  resetForm(): void {
    this.submitted = false;
    if (this.discussionForm) {
        this.discussionForm.reset();
    }
    this.contentEditor?.setMarkdown('');
    this.selectedImages = null;
    this.selectedAttachments = null;
    this.contentError = null;
    // this.generalError = null; // Consider clearing generalError based on context
  }

  onCancel(): void {
      if (this.forumId !== undefined && this.forumId !== null && !isNaN(this.forumId)) {
        this.router.navigate(['/app/forums', this.forumId, 'view']);
      } else {
        this.router.navigate(['/app/forums/tree-table']);
      }
  }
}
