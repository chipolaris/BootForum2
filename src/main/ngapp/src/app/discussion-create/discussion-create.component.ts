import { Component, OnInit, Output, EventEmitter, inject, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormControl } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

// Import Toast UI Editor specific modules
import { Editor } from '@toast-ui/editor';
import { DiscussionService } from '../_services/discussion.service';
import { ForumService } from '../_services/forum.service';
import { TagService } from '../_services/tag.service';
import { ConfigService } from '../_services/config.service'; // Import ConfigService
import { FileValidationService, FileValidationError } from '../_services/file-validation.service'; // Import new validation service
import { DiscussionDTO, TagDTO } from '../_data/dtos';
import { finalize } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { MultiSelectModule } from 'primeng/multiselect';

@Component({
  selector: 'app-discussion-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MultiSelectModule
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
  availableTags: TagDTO[] = [];
  selectedImages: FileList | null = null;
  selectedAttachments: FileList | null = null;
  submitted = false;
  isLoading = true;
  contentError: string | null = null;
  generalError: string | null = null;

  // New properties for validation errors
  imageErrors: FileValidationError[] = [];
  attachmentErrors: FileValidationError[] = [];

  // New properties for validation config with defaults
  public validationConfig = {
    content: {
      posts: { minLength: 10, maxLength: 20000 },
      tags: { maxTagsPerPost: 3 }
    },
    images: {
      maxFileSizeMB: 5,
      allowedTypes: ['jpg', 'png', 'gif', 'jpeg'],
    },
    attachments: {
      maxFileSizeMB: 10,
      allowedTypes: ['pdf', 'zip', 'doc', 'docx'],
    }
  };

  private fb = inject(FormBuilder);
  private discussionService = inject(DiscussionService);
  private forumService = inject(ForumService);
  private tagService = inject(TagService);
  private configService = inject(ConfigService);
  private fileValidationService = inject(FileValidationService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private subscriptions = new Subscription();

  ngOnInit(): void {
    this.loadValidationConfig(); // Start by loading config
  }

  /**
   * REFACTORED: Use the more efficient getSettings() method to fetch all
   * required configuration in a single network request.
   */
  private loadValidationConfig(): void {
    this.isLoading = true;
    const requiredSettings = [
      'content.posts.minLength',
      'content.posts.maxLength',
      'content.tags.maxTagsPerPost',
      'images.maxFileSizeMB',
      'images.allowedTypes',
      'attachments.maxFileSizeMB',
      'attachments.allowedTypes'
    ];

    const configSub = this.configService.getSettings(requiredSettings).subscribe({
      next: (settingsMap) => {
        this.validationConfig.content.posts.minLength = settingsMap.get('content.posts.minLength') ?? this.validationConfig.content.posts.minLength;
        this.validationConfig.content.posts.maxLength = settingsMap.get('content.posts.maxLength') ?? this.validationConfig.content.posts.maxLength;
        this.validationConfig.content.tags.maxTagsPerPost = settingsMap.get('content.tags.maxTagsPerPost') ?? this.validationConfig.content.tags.maxTagsPerPost;
        this.validationConfig.images.maxFileSizeMB = settingsMap.get('images.maxFileSizeMB') ?? this.validationConfig.images.maxFileSizeMB;
        this.validationConfig.images.allowedTypes = settingsMap.get('images.allowedTypes') ?? this.validationConfig.images.allowedTypes;
        this.validationConfig.attachments.maxFileSizeMB = settingsMap.get('attachments.maxFileSizeMB') ?? this.validationConfig.attachments.maxFileSizeMB;
        this.validationConfig.attachments.allowedTypes = settingsMap.get('attachments.allowedTypes') ?? this.validationConfig.attachments.allowedTypes;

        this.loadInitialData(); // Proceed with original logic
      },
      error: (err) => {
        console.error("Failed to load validation configuration, using defaults.", err);
        this.loadInitialData(); // Proceed with defaults on error
      }
    });

    this.subscriptions.add(configSub);
  }

  private loadInitialData(): void {
    const routeSub = this.route.paramMap.subscribe(params => {
      const idParam = params.get('forumId');
      if (idParam) {
        const parsedId = +idParam;
        if (!isNaN(parsedId)) {
          this.forumId = parsedId;
          this.fetchForumTitleAndPrepareForm();
          this.loadAvailableTags();
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
    this.subscriptions.add(routeSub);
  }

  private fetchForumTitleAndPrepareForm(): void {
    this.isLoading = true;
    const forumTitleSub = this.forumService.getForumById(this.forumId).subscribe({
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
    this.subscriptions.add(forumTitleSub);
  }

  private setupForm(): void {
    this.discussionForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(150)]],
      content: [''],
      tagIds: [[] as number[], [this.maxTagsValidator(this.validationConfig.content.tags.maxTagsPerPost)]]
    });
    this.isLoading = false;
  }

  private loadAvailableTags(): void {
    const tagsSub = this.tagService.getAllTags().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.availableTags = response.data.filter(tag => !tag.disabled);
        } else {
          console.error('Failed to load tags for discussion creation.');
        }
      },
      error: (err) => console.error('Error fetching tags:', err)
    });
    this.subscriptions.add(tagsSub);
  }

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
    if (this.contentEditor) return;
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
     this.subscriptions.unsubscribe();
  }

  get f() { return this.discussionForm.controls; }

  onImageChange(event: Event): void {
    const element = event.target as HTMLInputElement;
    this.selectedImages = element.files;
    this.imageErrors = [];
    if (this.selectedImages) {
      this.imageErrors = this.fileValidationService.validateFiles(
        this.selectedImages,
        this.validationConfig.images.maxFileSizeMB,
        this.validationConfig.images.allowedTypes
      );
    }
  }

  onAttachmentChange(event: Event): void {
    const element = event.target as HTMLInputElement;
    this.selectedAttachments = element.files;
    this.attachmentErrors = [];
    if (this.selectedAttachments) {
      this.attachmentErrors = this.fileValidationService.validateFiles(
        this.selectedAttachments,
        this.validationConfig.attachments.maxFileSizeMB,
        this.validationConfig.attachments.allowedTypes
      );
    }
  }

  private validatePost(): boolean {
    const discussionContent = this.contentEditor?.getMarkdown() || '';
    const contentLength = new TextEncoder().encode(discussionContent).length;

    if (contentLength < this.validationConfig.content.posts.minLength) {
      this.contentError = `Content is too short. Minimum length is ${this.validationConfig.content.posts.minLength} characters (bytes).`;
      return false;
    }
    if (contentLength > this.validationConfig.content.posts.maxLength) {
      this.contentError = `Content is too long. Maximum length is ${this.validationConfig.content.posts.maxLength} characters (bytes).`;
      return false;
    }

    if (this.discussionForm.invalid) {
      Object.values(this.discussionForm.controls).forEach(control => control.markAsTouched());
      return false;
    }

    if (this.imageErrors.length > 0 || this.attachmentErrors.length > 0) {
      this.generalError = "Please fix the errors in the uploaded files before submitting.";
      return false;
    }

    return true;
  }

  onSubmit(): void {
    if (this.forumId === undefined || this.forumId === null || isNaN(this.forumId)) {
      this.generalError = 'Cannot create discussion: Forum ID is missing or invalid.';
      return;
    }

    this.submitted = true;
    this.contentError = null;
    this.generalError = null;

    if (!this.validatePost()) {
      return;
    }

    this.isLoading = true;
    const formData = new FormData();
    const discussionContent = this.contentEditor?.getMarkdown() || '';

    formData.append('forumId', this.forumId.toString());
    formData.append('title', this.f['title'].value);
    formData.append('content', discussionContent);

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

    const submitSub = this.discussionService.createDiscussion(formData)
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            this.discussionCreationResult.emit({ success: true, data: response.data });
            this.router.navigate(['/app/discussions', response.data.id, 'view']);
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
    this.subscriptions.add(submitSub);
  }

  resetForm(): void {
    this.submitted = false;
    if (this.discussionForm) {
        this.discussionForm.reset({ tagIds: [] });
    }
    this.contentEditor?.setMarkdown('');
    this.selectedImages = null;
    this.selectedAttachments = null;
    this.contentError = null;
    this.imageErrors = [];
    this.attachmentErrors = [];
  }

  onCancel(): void {
      if (this.forumId !== undefined && this.forumId !== null && !isNaN(this.forumId)) {
        this.router.navigate(['/app/forums', this.forumId, 'view']);
      } else {
        this.router.navigate(['/app/forums/tree-table']);
      }
  }
}
