import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileInfoDTO } from '../_data/dtos';
import { FileService } from '../_services/file.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-file-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './file-list.component.html',
  styleUrls: ['./file-list.component.css'] // or use inline styles
})
export class FileListComponent {
  @Input() files: FileInfoDTO[] = [];
  private fileService = inject(FileService);

  getFileIcon(mimeType: string): string {
    if (mimeType.startsWith('image/')) return '🖼️';
    if (mimeType === 'application/pdf') return '📄';
    if (mimeType.startsWith('video/')) return '🎞️';
    if (mimeType.startsWith('audio/')) return '🎵';
    return '📁';
  }

  formatFileSize(size: number): string {
    if (size < 1024) return `${size} B`;
    else if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    else return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }

  triggerDownload(file: FileInfoDTO): void {
    if (!file.id) {
      console.error('File ID is missing, cannot download.');
      // Optionally, show a user-friendly error message
      return;
    }
    this.fileService.downloadFile(file.id, file.originalFilename).subscribe({
      next: () => {
        console.log(`Download initiated for ${file.originalFilename}`);
        // Optionally, show a success message
      },
      error: (err: HttpErrorResponse) => {
        console.error(`Error downloading file ${file.originalFilename}:`, err);
        // Handle error: show a user-friendly message
        // e.g., based on err.status or err.message
        alert(`Failed to download ${file.originalFilename}. Status: ${err.status}`);
      }
    });
  }
}

