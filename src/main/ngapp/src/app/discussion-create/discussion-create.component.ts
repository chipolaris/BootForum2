import { Component, OnInit, Output, EventEmitter, inject, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormControl } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

// Import Toast UI Editor specific modules
import { Editor } from '@toast-ui/editor';
import { DiscussionService } from '../_services/discussion.service';
import { ForumService } from '../_services/forum.service';
import { TagService } from '../_services/tag.service'; // Import TagService
import { DiscussionDTO, TagDTO } from '../_data/dtos';
import { finalize } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { MultiSelectModule } from 'primeng/multiselect'; // Import MultiSelectModule

@Component({
  selector: 'app-discussion-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MultiSelectModule // Add MultiSelectModule
  ],
  templateUrl: './discussion-create.component.html',
  styleUrls: ['./discussion-create.component.css']
})
export class DiscussionCreateComponent implements OnInit, OnDestroy {

  forumId!: number;
  forumTitle: string | null = null;
  @Output() discussionCreationResult = new EventEmitter<{ success: boolean, data?: DiscussionDTO | null, error?: string }>();

  contentEditor: InstanceType<typeof Editor> | null = null;

  @ViewChild('contentEditorRef') private set editorContentEl(el: ElementRef | undefined) {
    if (el && el.nativeElement && !this.contentEditor) {
      this.initializeEditor(el.nativeElement);
    }
  }

  discussionForm!: FormGroup;
  availableTags: TagDTO[] = []; // Property to hold available tags
  selectedImages: FileList | null = null;
  selectedAttachments: FileList | null = null;
  submitted = false;
  isLoading = true;
  contentError: string | null = null;
  generalError: string | null = null;

  private fb = inject(FormBuilder);
  private discussionService = inject(DiscussionService);
  private forumService = inject(ForumService);
  private tagService = inject(TagService); // Inject TagService
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
          this.loadAvailableTags(); // Load tags when component initializes
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
        this.setupForm();
      },
      error: (err) => {
        console.error('Error fetching forum details for title:', err);
        this.generalError = 'Failed to load forum details. You can still create a discussion.';
        this.setupForm();
      }
    });
  }

  private setupForm(): void {
    this.discussionForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(150)]],
      content: [''],
      // Add tagIds control with custom validator
      tagIds: [[] as number[], [this.maxTagsValidator(3)]]
    });
    this.isLoading = false;
  }

  private loadAvailableTags(): void {
    this.tagService.getAllTags().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          // Only allow selection of active (not disabled) tags
          this.availableTags = response.data.filter(tag => !tag.disabled);
        } else {
          console.error('Failed to load tags for discussion creation.');
        }
      },
      error: (err) => console.error('Error fetching tags:', err)
    });
  }

  // Custom validator to enforce a maximum number of selected tags
  private maxTagsValidator(max: number) {
    return (control: FormControl): { [key: string]: any } | null => {
      const selected = control.value as any[];
      if (selected && selected.length > max) {
        return { 'maxTagsExceeded': { max: max, actual: selected.length } };
      }
      return null;
    };
  }

  private initializeEditor(element: HTMLElement): void {
    if (this.contentEditor) {
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

    // Append tagIds to FormData
    const tagIds = this.f['tagIds'].value as number[];
    if (tagIds && tagIds.length > 0) {
      tagIds.forEach(id => formData.append('tagIds', id.toString()));
    }

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
            if (response.data.id) {
                this.router.navigate(['/app/forums', this.forumId, 'view']);
            } else {
                this.router.navigate(['/app/forums', this.forumId, 'view']);
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
        this.discussionForm.reset({ tagIds: [] }); // Reset tags to an empty array
    }
    this.contentEditor?.setMarkdown('');
    this.selectedImages = null;
    this.selectedAttachments = null;
    this.contentError = null;
  }

  onCancel(): void {
      if (this.forumId !== undefined && this.forumId !== null && !isNaN(this.forumId)) {
        this.router.navigate(['/app/forums', this.forumId, 'view']);
      } else {
        this.router.navigate(['/app/forums/tree-table']);
      }
  }
}
