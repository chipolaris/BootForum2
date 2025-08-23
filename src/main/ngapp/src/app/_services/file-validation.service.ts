import { Injectable } from '@angular/core';

export interface FileValidationError {
  fileName: string;
  error: string;
}

@Injectable({
  providedIn: 'root'
})
export class FileValidationService {

  constructor() { }

  /**
   * Validates a list of files against size and type constraints.
   * @param files The FileList to validate.
   * @param maxSizeMB The maximum allowed file size in megabytes.
   * @param allowedTypes An array of allowed file extensions (e.g., ['jpg', 'png']).
   * @returns An array of validation errors. Returns an empty array if all files are valid.
   */
  validateFiles(
    files: FileList,
    maxSizeMB: number,
    allowedTypes: string[] | string | { [key: number]: string }
  ): FileValidationError[] {
    const errors: FileValidationError[] = [];

    let typesArray: string[];

    // --- Start of Fix ---
    // Make the service robust by handling multiple possible formats for allowedTypes.
    if (Array.isArray(allowedTypes)) {
      // Case 1: It's a proper JavaScript array. Use it directly.
      typesArray = allowedTypes;
    } else if (typeof allowedTypes === 'object' && allowedTypes !== null) {
      // Case 2: It's an array-like object like {0: 'doc', 1: 'docx'}. Convert it to a real array.
      typesArray = Object.values(allowedTypes);
    } else if (typeof allowedTypes === 'string' && allowedTypes.length > 0) {
      // Case 3: It's a comma-separated string. Convert it to an array.
      typesArray = allowedTypes.split(',').map(t => t.trim()).filter(t => t);
    } else {
      // Fallback: It's null, undefined, or an empty string. Treat as no types specified.
      typesArray = [];
    }
    // --- End of Fix ---

    if (typesArray.length === 0) {
      // If no types are specified, we can't validate, so assume it's a configuration issue and allow.
      return errors;
    }

    const allowedTypesLower = typesArray.map(t => t.toLowerCase());
    const maxSizeBytes = maxSizeMB * 1024 * 1024;

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const fileExtension = file.name.split('.').pop()?.toLowerCase();

      if (!fileExtension || !allowedTypesLower.includes(fileExtension)) {
        errors.push({
          fileName: file.name,
          error: `Invalid file type. Allowed types are: ${typesArray.join(', ')}.`
        });
        continue; // Don't check size if type is invalid
      }

      if (file.size > maxSizeBytes) {
        errors.push({
          fileName: file.name,
          error: `File is too large. Maximum size is ${maxSizeMB} MB.`
        });
      }
    }

    return errors;
  }
}
