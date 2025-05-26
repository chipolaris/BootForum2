import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ApiResponse, DiscussionDTO } from '../_data/dtos'; // Adjust path as needed

@Injectable({
  providedIn: 'root'
})
export class DiscussionService {
  private http = inject(HttpClient);
  private baseApiUrl = '/api/user/discussions'; // Matches your DiscussionController @RequestMapping

  constructor() { }

  /**
   * Submits the discussion creation data (including files) to the backend.
   * @param formData The FormData object containing discussion details and files.
   * @returns An Observable of ApiResponse containing the created DiscussionDTO.
   */
  createDiscussion(formData: FormData): Observable<ApiResponse<DiscussionDTO>> {
    return this.http.post<ApiResponse<DiscussionDTO>>(`${this.baseApiUrl}/create`, formData)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log('Discussion created successfully via service', response.data);
          } else {
            console.error('Discussion creation failed via service', response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('An error occurred in DiscussionService:', error);
    let errorMessage = 'Something bad happened; please try again later.';

    if (error.error instanceof ErrorEvent) {
      // Client-side or network error
      errorMessage = `An error occurred: ${error.error.message}`;
    } else if (error.error && error.error.message) {
      // Backend returned a structured error response (ApiResponse)
      errorMessage = `Error ${error.status}: ${error.error.message}`;
      if (error.error.errors && Array.isArray(error.error.errors)) {
        errorMessage += ` Details: ${error.error.errors.join(', ')}`;
      }
    } else {
      // Other types of server-side errors
      errorMessage = `Server returned code ${error.status}, error message is: ${error.message}`;
    }
    return throwError(() => new Error(errorMessage));
  }
}
