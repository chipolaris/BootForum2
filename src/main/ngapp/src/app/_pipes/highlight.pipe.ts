import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
  name: 'highlight',
  standalone: true
})
export class HighlightPipe implements PipeTransform {

  private sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined, keyword: string): SafeHtml | string {
    if (!value || !keyword?.trim()) {
      return value || '';
    }

    // Use a regular expression for case-insensitive, global replacement.
    // The escapeRegExp helper prevents errors if the keyword contains special regex characters.
    const re = new RegExp(this.escapeRegExp(keyword.trim()), 'gi');
    const highlightedValue = value.replace(re, match =>
      `<mark class="bg-yellow-300 dark:bg-yellow-500 text-black rounded px-1 py-0.5">${match}</mark>`
    );

    // Sanitize the HTML to prevent XSS attacks before rendering.
    return this.sanitizer.bypassSecurityTrustHtml(highlightedValue);
  }

  /**
   * Escapes special characters in a string to be used in a regular expression.
   */
  private escapeRegExp(string: string): string {
    // $& means the whole matched string
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }
}
