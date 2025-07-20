import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ApiResponse, CommentDTO, Page, CommentThreadDTO } from '../_data/dtos'; // Adjust path as needed

@Injectable({
  providedIn: 'root'
})
export class CommentService {
  private http = inject(HttpClient);
  private basePublicApiUrl = '/api/public';
  private baseUserApiUrl = '/api/user'; // For authenticated actions

  constructor() { }

  /**
   * Fetches a paginated and sorted list of ALL comments for a specific discussion.
   */
  listComments(
    discussionId: number,
    page: number = 0,
    size: number = 10,
    sortProperty: string = 'createDate',
    sortDirection: string = 'ASC'
  ): Observable<ApiResponse<Page<CommentDTO>>> {

    let params = new HttpParams()
      // discussionId is part of the path for this endpoint
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', `${sortProperty},${sortDirection}`);

    const commentsUrl = `${this.basePublicApiUrl}/comments/by-discussion/${discussionId}`;

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

  /**
   * Fetches a single comment by its ID.
   * @param id The ID of the comment to retrieve.
   * @returns An Observable of ApiResponse containing the CommentDTO.
   */
  getCommentById(id: number): Observable<ApiResponse<CommentDTO>> {
    // Assuming a public endpoint exists or will be created for fetching a single comment
    // If not, this might need adjustment or the comment might be sourced differently for quoting
    const url = `${this.basePublicApiUrl}/comments/${id}`; // Placeholder URL, adjust if backend endpoint differs
    return this.http.get<ApiResponse<CommentDTO>>(url)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log(`Comment with id ${id} fetched successfully via service`, response.data);
          } else {
            console.error(`Failed to fetch comment with id ${id} via service`, response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Submits the comment creation data (including files) to the backend.
   * @param formData The FormData object containing comment details and files.
   * @returns An Observable of ApiResponse containing the created CommentDTO.
   */
  createComment(formData: FormData): Observable<ApiResponse<CommentDTO>> {
    return this.http.post<ApiResponse<CommentDTO>>(`${this.baseUserApiUrl}/comments/create`, formData)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log('Comment created successfully via service', response.data);
          } else {
            console.error('Comment creation failed via service', response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Fetches a comment thread, which includes the parent discussion and the
   * chain of comments leading to the specified comment.
   * @param commentId The ID of the comment to start the thread from.
   */
  getCommentThread(commentId: number): Observable<ApiResponse<CommentThreadDTO>> {
    const url = `${this.basePublicApiUrl}/comments/${commentId}/thread`;
    return this.http.get<ApiResponse<CommentThreadDTO>>(url)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log(`Comment thread for comment ${commentId} fetched successfully.`, response.data);
          } else {
            console.error(`Failed to fetch comment thread for comment ${commentId}.`, response.message, response.errors);
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
