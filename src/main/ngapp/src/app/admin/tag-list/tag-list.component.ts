import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TagService } from '../../_services/tag.service';
import { TagCreateDTO, TagDTO, TagUpdateDTO, errorMessageFromApiResponse } from '../../_data/dtos';

// PrimeNG Modules
import { TableModule, TableRowReorderEvent } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { TagModule as PrimeTagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { NgIcon, provideIcons } from '@ng-icons/core';

// Local Components
import { TagFormComponent } from '../tag-form/tag-form.component';

@Component({
  selector: 'app-tag-list',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    ToastModule,
    ConfirmDialogModule,
    DynamicDialogModule,
    PrimeTagModule,
    TooltipModule,
    NgIcon
  ],
  // These services should be provided globally in app.config.ts for a real app.
  // Providing them here makes the component self-contained for this example.
  providers: [DialogService, ConfirmationService, MessageService],
  templateUrl: './tag-list.component.html',
  styleUrls: ['./tag-list.component.css']
})
export class TagListComponent implements OnInit {
  private tagService = inject(TagService);
  private dialogService = inject(DialogService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);

  tags: TagDTO[] = [];
  isLoading = true;

  ngOnInit(): void {
    this.loadTags();
  }

  loadTags(): void {
    this.isLoading = true;
    this.tagService.getAllTags().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.tags = response.data;
        } else {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessageFromApiResponse(response) || 'Failed to load tags.' });
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.messageService.add({ severity: 'error', summary: 'Server Error', detail: err.message || 'An unexpected error occurred.' });
      }
    });
  }

  openCreateDialog(): void {
    const ref = this.dialogService.open(TagFormComponent, {
      header: 'Create New Tag',
      width: '30rem'
    });

    ref.onClose.subscribe((formData: Omit<TagDTO, 'id' | 'sortOrder' | 'disabled'>) => {
      if (formData) {
        const payload: TagCreateDTO = {
          label: formData.label,
          icon: formData.icon,
          iconColor: formData.iconColor
        };
        this.tagService.createTag(payload).subscribe(res => {
          if (res.success) {
            this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Tag created successfully.' });
            this.loadTags();
          } else {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessageFromApiResponse(res) || 'Failed to create tag.' });
          }
        });
      }
    });
  }

  openEditDialog(tag: TagDTO): void {
    const ref = this.dialogService.open(TagFormComponent, {
      header: `Edit Tag: ${tag.label}`,
      width: '30rem',
      data: { tag }
    });

    ref.onClose.subscribe((formData: Omit<TagDTO, 'id' | 'sortOrder'>) => {
      if (formData) {
        const payload: TagUpdateDTO = {
          id: tag.id,
          label: formData.label,
          icon: formData.icon,
          iconColor: formData.iconColor,
          disabled: formData.disabled
        };
        this.tagService.updateTag(tag.id, payload).subscribe(res => {
          if (res.success) {
            this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Tag updated successfully.' });
            this.loadTags();
          } else {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessageFromApiResponse(res) || 'Failed to update tag.' });
          }
        });
      }
    });
  }

  onDelete(tag: TagDTO): void {
    this.confirmationService.confirm({
      message: `Are you sure you want to delete the tag "${tag.label}"? This cannot be undone.`,
      header: 'Confirm Deletion',
      icon: 'pi pi-exclamation-triangle',
      accept: () => {
        this.tagService.deleteTag(tag.id).subscribe(res => {
          if (res.success) {
            this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Tag deleted successfully.' });
            this.loadTags();
          } else {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessageFromApiResponse(res) || 'Failed to delete tag.' });
          }
        });
      }
    });
  }

  onRowReorder(event: TableRowReorderEvent): void {
    // The 'this.tags' array is already updated by PrimeNG at this point.
    // We just need to send the new order of IDs to the backend.
    const newOrderIds = this.tags.map(t => t.id);
    this.tagService.updateTagOrder(newOrderIds).subscribe({
      next: (res) => {
        if (res.success) {
          this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Tag order updated.' });
        } else {
          this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Failed to update tag order.' });
          this.loadTags(); // Revert to original order on failure
        }
      },
      error: (err) => {
        this.messageService.add({ severity: 'error', summary: 'Server Error', detail: 'Could not save new order.' });
        this.loadTags(); // Revert to original order on failure
      }
    });
  }

  getStatusSeverity(disabled: boolean): "success" | "danger" {
    return disabled ? 'danger' : 'success';
  }
}
