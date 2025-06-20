import { Component, OnInit, OnDestroy, ViewChild, ElementRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable, Subscription, of } from 'rxjs';
import { finalize, switchMap, catchError, tap } from 'rxjs/operators';

import { Editor } from '@toast-ui/editor';

import { CommentService } from '../_services/comment.service';
import { DiscussionService } from '../_services/discussion.service'; // To fetch discussion content for quoting
import { CommentDTO, DiscussionDTO, ApiResponse } from '../_data/dtos'; // Added DiscussionDTO

interface QuoteState {
  contentToQuote?: string;
  authorToQuote?: string;
  quotedItemTitle?: string; // Title of the discussion or comment being quoted
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
  discussionTitle: string | null = null; // Title of the parent discussion

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

  private fb = inject(FormBuilder);
  private commentService = inject(CommentService);
  private discussionService = inject(DiscussionService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private subscriptions = new Subscription();

  private quoteStateData: QuoteState | null = null;
  private quotedContentToSet: string = ''; // Temporary holder for quote content

  constructor() {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras.state) {
      this.quoteStateData = navigation.extras.state as QuoteState;
      console.log('Received quote state data:', this.quoteStateData);
    }
  }

  ngOnInit(): void {
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
          // Fetch discussion title for context, only if not already available from quoteStateData (when quoting main discussion)
          if (this.isQuote && !this.replyToId && this.quoteStateData?.quotedItemTitle) {
            this.discussionTitle = this.quoteStateData.quotedItemTitle;
            return of(null); // No need to fetch discussion if title is from state
          }
          return this.discussionService.getDiscussionById(this.discussionId as number);
        }),
        catchError(err => {
          if (!this.discussionTitle) { // Only set error if title wasn't set from state
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
            this.setupForm(); // Initialize form

            if (this.isQuote) {
              if (this.quoteStateData?.contentToQuote && this.quoteStateData?.authorToQuote) {
                console.log('Using quote data from state.');
                const formattedQuote = `> **${this.quoteStateData.authorToQuote} wrote:**\n>\n${this.quoteStateData.contentToQuote.split('\n').map(line => `> ${line}`).join('\n')}\n\n`;
                if (this.contentEditor) {
                  this.contentEditor.setMarkdown(formattedQuote);
                } else {
                  this.quotedContentToSet = formattedQuote;
                }
                this.isLoading = false; // Content is set from state
              } else {
                console.log('Quote mode, but no state data. Falling back to API call.');
                this.loadContentForQuotingViaApi(); // Fallback if state is missing
              }
            } else {
              this.isLoading = false; // Not quoting
            }
          })
        );
      })
    );
  }

  private setupForm(): void {
    let initialTitle = `Re: ${this.discussionTitle || 'Discussion'}`;

    if (this.replyToId) { // Replying to a specific comment
      if (this.isQuote && this.quoteStateData?.quotedItemTitle) {
        initialTitle = `Re: ${this.quoteStateData.quotedItemTitle}`;
      } else {
        // If not quoting or title not in state, generate a generic reply title
        initialTitle = `Re: Comment in "${this.discussionTitle || 'Discussion'}"`;
      }
    } else if (this.isQuote && this.quoteStateData?.quotedItemTitle) { // Quoting the main discussion
        initialTitle = `Re: ${this.quoteStateData.quotedItemTitle}`;
    }


    this.commentForm = this.fb.group({
      title: [initialTitle, [Validators.required, Validators.maxLength(255)]],
    });
    // isLoading might be set to false here or after quote content is processed
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
      console.log('Toast UI Editor initialized for comments.');
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

    // Explicitly type sourceObservable
    let sourceObservable: Observable<ApiResponse<CommentDTO | DiscussionDTO>>;

    if (this.replyToId) {
      sourceObservable = this.commentService.getCommentById(this.replyToId);
    } else {
      // Ensure discussionId is treated as number if its type is number | null
      sourceObservable = this.discussionService.getDiscussionById(this.discussionId as number);
    }

    // Explicitly type the piped observable
    const pipedObservable: Observable<ApiResponse<CommentDTO | DiscussionDTO>> = sourceObservable.pipe(
      finalize(() => this.isLoading = false)
    );

    this.subscriptions.add(
      pipedObservable.subscribe({ // Now subscribe is called on the explicitly typed observable
        next: (response) => {
          let contentToQuote = '';
          let authorToQuote = '';
          if (response && response.success && response.data) {
            contentToQuote = response.data.content;
            authorToQuote = response.data.createBy;
            // Also update discussionTitle if quoting main discussion and it wasn't set from state
            // Check if response.data has a title property before accessing it
            const responseDataWithTitle = response.data as DiscussionDTO; // Or a common interface if applicable
            if (!this.replyToId && responseDataWithTitle.title && !this.discussionTitle) {
                this.discussionTitle = responseDataWithTitle.title;
                // Potentially re-evaluate form title if it depends on discussionTitle
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
            console.error('API quote load failed:', response?.message);
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
  }

  onAttachmentChange(event: Event): void {
    const element = event.target as HTMLInputElement;
    this.selectedAttachments = element.files;
  }

  onSubmit(): void {
    if (!this.discussionId) {
      this.generalError = 'Discussion ID is missing. Cannot submit comment.';
      return;
    }

    this.submitted = true;
    this.contentError = null;
    this.generalError = null;

    const commentContent = this.contentEditor?.getMarkdown().trim() || '';
    if (!commentContent) {
      this.contentError = 'Comment content is required.';
      return;
    }

    if (this.commentForm.invalid) {
      Object.values(this.commentForm.controls).forEach(control => control.markAsTouched());
      return;
    }

    this.isSubmitting = true;
    const formData = new FormData();
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
                { fragment: `comment-${response.data.id}` }); // Add fragment to scroll to new comment
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
      this.router.navigate(['/app/forums/tree-table']); // Fallback
    }
  }
}
