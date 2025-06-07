import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ApiResponse, CommentDTO, Page } from '../_data/dtos'; // Adjust path as needed

@Injectable({
  providedIn: 'root'
})
export class CommentService {
  private http = inject(HttpClient);
  private basePublicApiUrl = '/api/public/discussion';

  constructor() { }

  /**
   * Fetches a paginated and sorted list of ALL comments for a specific discussion.
   * @param discussionId The ID of the discussion whose comments are to be retrieved.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of comments per page.
   * @param sortProperty The property to sort by.
   * @param sortDirection The direction of sorting ('ASC' or 'DESC').
   * @returns An Observable of ApiResponse containing a Page of CommentDTOs.
   */
  listComments(
    discussionId: number,
    page: number = 0,
    size: number = 10,
    sortProperty: string = 'createDate',
    sortDirection: string = 'ASC'
  ): Observable<ApiResponse<Page<CommentDTO>>> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', `${sortProperty},${sortDirection}`);

    const commentsUrl = `${this.basePublicApiUrl}/${discussionId}/comments`;

    return this.http.get<ApiResponse<Page<CommentDTO>>>(commentsUrl, { params })
      .pipe(
        tap(response => {
          if (response.success) {
            console.log(`All comments for discussion ${discussionId} listed successfully via service (listComments)`, response.data);
          } else {
            console.error(`Failed to list all comments for discussion ${discussionId} via service (listComments)`, response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('An error occurred in CommentService:', error);
    let errorMessage = 'Something bad happened; please try again later.';

    if (error.error instanceof ErrorEvent) {
      errorMessage = `An error occurred: ${error.error.message}`;
    } else if (error.error && error.error.message) {
      errorMessage = `Error ${error.status}: ${error.error.message}`;
      if (error.error.errors && Array.isArray(error.error.errors)) {
        errorMessage += ` Details: ${error.error.errors.join(', ')}`;
      }
    } else {
      errorMessage = `Server returned code ${error.status}, error message is: ${error.message}`;
    }
    return throwError(() => new Error(errorMessage));
  }
}
