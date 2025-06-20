// src/app/_services/file.service.ts (example)
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private http = inject(HttpClient);
  private baseUrl = '/api/public/files'; // Or your environment-specific base URL

  constructor() { }

  downloadFile(fileId: number, filename: string): Observable<void> {
    const headers = new HttpHeaders({
      // Add any necessary headers here, e.g., Authorization if needed
      // 'Authorization': 'Bearer ' + yourAuthToken
    });

    return this.http.get(`${this.baseUrl}/${fileId}`, {
      headers: headers,
      responseType: 'blob' // Important: tells HttpClient to expect binary data
    }).pipe(
      map(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        document.body.appendChild(a); // Append the element to the DOM
        a.style.display = 'none';
        a.href = url;
        a.download = filename; // Use the filename passed or retrieved from headers
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a); // Clean up by removing the element
      })
    );
  }
}
