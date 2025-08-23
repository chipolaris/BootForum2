import { Component, OnInit, OnDestroy, ViewChild, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subscription, of, forkJoin } from 'rxjs';
import { finalize, switchMap, catchError, tap } from 'rxjs/operators';

import { Editor } from '@toast-ui/editor';

import { CommentService } from '../_services/comment.service';
import { DiscussionService } from '../_services/discussion.service';
import { ConfigService } from '../_services/config.service';
import { FileValidationService, FileValidationError } from '../_services/file-validation.service';
import { CommentDTO, DiscussionDTO, ApiResponse } from '../_data/dtos';

interface QuoteState {
  contentToQuote?: string;
  authorToQuote?: string;
  quotedItemTitle?: string;
}

@Component({
  selector: 'app-comment-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './comment-create.component.html',
  styleUrls: ['./comment-create.component.css']
})
export class CommentCreateComponent implements OnInit, OnDestroy {
  commentForm!: FormGroup;
  discussionId: number | null = null;
  replyToId: number | null = null;
  isQuote = false;
  mode: 'reply' | 'quote' = 'reply';
  discussionTitle: string | null = null;

  contentEditor: InstanceType<typeof Editor> | null = null;
  @ViewChild('contentEditorRef') private set editorContentEl(el: ElementRef | undefined) {
    if (el && el.nativeElement && !this.contentEditor) {
      this.initializeEditor(el.nativeElement);
    }
  }

  selectedImages: FileList | null = null;
  selectedAttachments: FileList | null = null;
  submitted = false;
  isLoading = true;
  isSubmitting = false;
  contentError: string | null = null;
  generalError: string | null = null;

  imageErrors: FileValidationError[] = [];
  attachmentErrors: FileValidationError[] = [];

  private validationConfig = {
    content: { posts: { minLength: 5, maxLength: 10000 } },
    images: { maxFileSizeMB: 5, allowedTypes: ['jpg', 'png', 'gif', 'jpeg'] },
    attachments: { maxFileSizeMB: 5, allowedTypes: ['pdf', 'zip', 'doc', 'docx'] }
  };

  private fb = inject(FormBuilder);
  private commentService = inject(CommentService);
  private discussionService = inject(DiscussionService);
  private configService = inject(ConfigService);
  private fileValidationService = inject(FileValidationService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private subscriptions = new Subscription();

  private quoteStateData: QuoteState | null = null;
  private quotedContentToSet: string = '';

  constructor() {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state) {
      this.quoteStateData = navigation.extras.state as QuoteState;
    }
  }

  ngOnInit(): void {
    this.loadValidationConfig();
  }

  private loadValidationConfig(): void {
    this.isLoading = true;
    const configObservables = {
      minLength: this.configService.getSetting('content.posts.minLength'),
      maxLength: this.configService.getSetting('content.posts.maxLength'),
      maxImageSize: this.configService.getSetting('images.maxFileSizeMB'),
      allowedImageTypes: this.configService.getSetting('images.allowedTypes'),
      maxAttachmentSize: this.configService.getSetting('attachments.maxFileSizeMB'),
      allowedAttachmentTypes: this.configService.getSetting('attachments.allowedTypes')
    };

    this.subscriptions.add(
      forkJoin(configObservables).subscribe({
        next: (configs) => {
          this.validationConfig.content.posts.minLength = configs.minLength ?? this.validationConfig.content.posts.minLength;
          this.validationConfig.content.posts.maxLength = configs.maxLength ?? this.validationConfig.content.posts.maxLength;
          this.validationConfig.images.maxFileSizeMB = configs.maxImageSize ?? this.validationConfig.images.maxFileSizeMB;
          this.validationConfig.images.allowedTypes = configs.allowedImageTypes ?? this.validationConfig.images.allowedTypes;
          this.validationConfig.attachments.maxFileSizeMB = configs.maxAttachmentSize ?? this.validationConfig.attachments.maxFileSizeMB;
          this.validationConfig.attachments.allowedTypes = configs.allowedAttachmentTypes ?? this.validationConfig.attachments.allowedTypes;
          this.loadInitialData();
        },
        error: (err) => {
          console.error("Failed to load validation configuration, using defaults.", err);
          this.loadInitialData();
        }
      })
    );
  }

  private loadInitialData(): void {
    this.subscriptions.add(
      this.route.paramMap.pipe(
        tap(params => {
          const dId = params.get('discussionId');
          this.discussionId = dId ? +dId : null;
          const rId = params.get('replyToId');
          this.replyToId = rId ? +rId : null;
          if (!this.discussionId) {
            this.generalError = 'Discussion ID is missing. Cannot create comment.';
            this.isLoading = false;
            throw new Error(this.generalError);
          }
        }),
        switchMap(() => {
          if (this.isQuote && !this.replyToId && this.quoteStateData?.quotedItemTitle) {
            this.discussionTitle = this.quoteStateData.quotedItemTitle;
            return of(null);
          }
          return this.discussionService.getDiscussionById(this.discussionId as number);
        }),
        catchError(err => {
          if (!this.discussionTitle) {
            this.generalError = `Failed to load discussion details: ${err.message}`;
          }
          this.isLoading = false;
          return of(null);
        })
      ).subscribe(discussionResponseOrNull => {
        if (discussionResponseOrNull && discussionResponseOrNull.success && discussionResponseOrNull.data) {
          this.discussionTitle = discussionResponseOrNull.data.title;
        }
        this.subscriptions.add(
          this.route.queryParamMap.subscribe(queryParams => {
            this.isQuote = queryParams.get('quote') === 'true';
            this.mode = this.isQuote ? 'quote' : 'reply';
            this.setupForm();
            if (this.isQuote) {
              if (this.quoteStateData?.contentToQuote && this.quoteStateData?.authorToQuote) {
                const formattedQuote = `> **${this.quoteStateData.authorToQuote} wrote:**\n>\n${this.quoteStateData.contentToQuote.split('\n').map(line => `> ${line}`).join('\n')}\n\n`;
                if (this.contentEditor) {
                  this.contentEditor.setMarkdown(formattedQuote);
                } else {
                  this.quotedContentToSet = formattedQuote;
                }
                this.isLoading = false;
              } else {
                this.loadContentForQuotingViaApi();
              }
            } else {
              this.isLoading = false;
            }
          })
        );
      })
    );
  }

  private setupForm(): void {
    let initialTitle = `Re: ${this.discussionTitle || 'Discussion'}`;
    if (this.replyToId) {
      if (this.isQuote && this.quoteStateData?.quotedItemTitle) {
        initialTitle = `Re: ${this.quoteStateData.quotedItemTitle}`;
      } else {
        initialTitle = `Re: Comment in "${this.discussionTitle || 'Discussion'}"`;
      }
    } else if (this.isQuote && this.quoteStateData?.quotedItemTitle) {
        initialTitle = `Re: ${this.quoteStateData.quotedItemTitle}`;
    }
    this.commentForm = this.fb.group({
      title: [initialTitle, [Validators.required, Validators.maxLength(255)]],
    });
  }

  private initializeEditor(element: HTMLElement): void {
    if (this.contentEditor) return;
    try {
      this.contentEditor = new Editor({
        el: element,
        height: '250px',
        initialEditType: 'markdown',
        previewStyle: 'vertical'
      });
      this.contentEditor.on('change', () => {
        if (this.contentError && this.contentEditor?.getMarkdown().trim()) {
          this.contentError = null;
        }
      });
      if (this.quotedContentToSet) {
        this.contentEditor.setMarkdown(this.quotedContentToSet);
        this.quotedContentToSet = '';
      }
    } catch (e) {
      console.error('Error during Comment Editor instantiation:', e);
      this.generalError = 'Failed to initialize the text editor.';
      this.isLoading = false;
    }
  }

  private loadContentForQuotingViaApi(): void {
    if (!this.discussionId) {
      this.isLoading = false;
      return;
    }
    this.isLoading = true;
    let sourceObservable: Observable<ApiResponse<CommentDTO | DiscussionDTO>>;
    if (this.replyToId) {
      sourceObservable = this.commentService.getCommentById(this.replyToId);
    } else {
      sourceObservable = this.discussionService.getDiscussionById(this.discussionId as number);
    }
    const pipedObservable: Observable<ApiResponse<CommentDTO | DiscussionDTO>> = sourceObservable.pipe(
      finalize(() => this.isLoading = false)
    );
    this.subscriptions.add(
      pipedObservable.subscribe({
        next: (response) => {
          let contentToQuote = '';
          let authorToQuote = '';
          if (response && response.success && response.data) {
            contentToQuote = response.data.content;
            authorToQuote = response.data.createBy;
            const responseDataWithTitle = response.data as DiscussionDTO;
            if (!this.replyToId && responseDataWithTitle.title && !this.discussionTitle) {
                this.discussionTitle = responseDataWithTitle.title;
                if (this.commentForm && this.f['title'].value.includes('Discussion')) {
                    this.f['title'].setValue(`Re: ${this.discussionTitle}`);
                }
            }
            const formattedQuote = `> **${authorToQuote} wrote:**\n>\n${contentToQuote.split('\n').map(line => `> ${line}`).join('\n')}\n\n`;
            if (this.contentEditor) {
              this.contentEditor.setMarkdown(formattedQuote);
            } else {
              this.quotedContentToSet = formattedQuote;
            }
          } else {
            this.generalError = 'Could not load content for quoting via API.';
          }
        },
        error: (err) => {
          this.generalError = `Error loading content for quoting via API: ${err.message}`;
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.contentEditor?.destroy();
    this.subscriptions.unsubscribe();
  }

  get f() { return this.commentForm.controls; }

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
    const commentContent = this.contentEditor?.getMarkdown() || '';
    const contentLength = new TextEncoder().encode(commentContent).length;

    if (contentLength < this.validationConfig.content.posts.minLength) {
      this.contentError = `Content is too short. Minimum length is ${this.validationConfig.content.posts.minLength} characters (bytes).`;
      return false;
    }
    if (contentLength > this.validationConfig.content.posts.maxLength) {
      this.contentError = `Content is too long. Maximum length is ${this.validationConfig.content.posts.maxLength} characters (bytes).`;
      return false;
    }

    if (this.commentForm.invalid) {
      Object.values(this.commentForm.controls).forEach(control => control.markAsTouched());
      return false;
    }

    if (this.imageErrors.length > 0 || this.attachmentErrors.length > 0) {
      this.generalError = "Please fix the errors in the uploaded files before submitting.";
      return false;
    }

    return true;
  }

  onSubmit(): void {
    if (!this.discussionId) {
      this.generalError = 'Discussion ID is missing. Cannot submit comment.';
      return;
    }

    this.submitted = true;
    this.contentError = null;
    this.generalError = null;

    if (!this.validatePost()) {
      return;
    }

    this.isSubmitting = true;
    const formData = new FormData();
    const commentContent = this.contentEditor?.getMarkdown() || '';
    formData.append('discussionId', this.discussionId.toString());
    if (this.replyToId !== null) {
      formData.append('replyToId', this.replyToId.toString());
    }
    formData.append('title', this.f['title'].value);
    formData.append('content', commentContent);

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

    this.subscriptions.add(
      this.commentService.createComment(formData)
        .pipe(finalize(() => this.isSubmitting = false))
        .subscribe({
          next: (response) => {
            if (response.success && response.data) {
              this.router.navigate(['/app/discussions', this.discussionId, 'view'],
                { fragment: `comment-${response.data.id}` });
            } else {
              this.generalError = response.message || 'Failed to create comment.';
               if(response.errors && response.errors.length > 0) {
                this.generalError += ` Details: ${response.errors.join(', ')}`;
            }
            }
          },
          error: (err) => {
            this.generalError = err.message || 'An unexpected error occurred while creating the comment.';
          }
        })
    );
  }

  onCancel(): void {
    if (this.discussionId) {
      this.router.navigate(['/app/discussions', this.discussionId, 'view']);
    } else {
      this.router.navigate(['/app/forums/tree-table']);
    }
  }
}
