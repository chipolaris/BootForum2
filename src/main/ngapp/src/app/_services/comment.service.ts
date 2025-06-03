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
  // The base URL matches the @RequestMapping in your CommentController
  private basePublicApiUrl = '/api/public';

  constructor() { }

  /**
   * Fetches a paginated and sorted list of comments for a specific discussion.
   * @param discussionId The ID of the discussion whose comments are to be retrieved.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of comments per page.
   * @param sortProperty The property to sort by.
   * @param sortDirection The direction of sorting ('ASC' or 'DESC').
   * @returns An Observable of ApiResponse containing a Page of CommentDTOs.
   */
  listComments(
    discussionId: number, // discussionId is required
    page: number = 0,
    size: number = 10,
    sortProperty: string = 'createDate', // Default sort as per your CommentController
    sortDirection: string = 'ASC'      // Default direction as per your CommentController
  ): Observable<ApiResponse<Page<CommentDTO>>> {

    let params = new HttpParams()
      .set('discussionId', discussionId.toString()) // Add discussionId as a request parameter
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', `${sortProperty},${sortDirection}`);

    // The URL for listing comments, based on your CommentController's @GetMapping("/comments")
    const commentsUrl = `${this.basePublicApiUrl}/comments`;

    return this.http.get<ApiResponse<Page<CommentDTO>>>(commentsUrl, { params })
      .pipe(
        tap(response => {
          if (response.success) {
            console.log(`Comments for discussion ${discussionId} listed successfully via service`, response.data);
          } else {
            console.error(`Failed to list comments for discussion ${discussionId} via service`, response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('An error occurred in CommentService:', error);
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
