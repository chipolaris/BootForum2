import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http'; // Added HttpParams
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ApiResponse, DiscussionDTO, DiscussionSummaryDTO, Page } from '../_data/dtos'; // Adjust path as needed, Added Page

@Injectable({
  providedIn: 'root'
})
export class DiscussionService {
  private http = inject(HttpClient);
  private baseUserApiUrl = '/api/user/discussions';
  private basePublicApiUrl = '/api/public/discussions'; // Matches your DiscussionController @RequestMapping

  constructor() { }

  /**
   * Submits the discussion creation data (including files) to the backend.
   * @param formData The FormData object containing discussion details and files.
   * @returns An Observable of ApiResponse containing the created DiscussionDTO.
   */
  createDiscussion(formData: FormData): Observable<ApiResponse<DiscussionDTO>> {
    return this.http.post<ApiResponse<DiscussionDTO>>(`${this.baseUserApiUrl}/create`, formData)
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

  /**
   * Fetches a paginated and sorted list of discussions.
   * @param forumId Optional ID of the forum to filter discussions.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of discussions per page.
   * @param sortProperty The property to sort by.
   * @param sortDirection The direction of sorting ('ASC' or 'DESC').
   * @returns An Observable of ApiResponse containing a Page of DiscussionDTOs.
   */
  listDiscussionSummaries(
    forumId?: number | null,
    page: number = 0,
    size: number = 10,
    sortProperty: string = 'stat.lastComment.commentDate', // Default sort
    sortDirection: string = 'DESC' // Default direction
  ): Observable<ApiResponse<Page<DiscussionSummaryDTO>>> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', `${sortProperty},${sortDirection}`); // Backend expects 'sort=property,direction'

    if (forumId !== null && forumId !== undefined) {
      params = params.append('forumId', forumId.toString());
    }

    return this.http.get<ApiResponse<Page<DiscussionSummaryDTO>>>(`${this.basePublicApiUrl}/list`, { params })
      .pipe(
        tap(response => {
          if (response.success) {
            console.log('Discussions listed successfully via service', response.data);
          } else {
            console.error('Failed to list discussions via service', response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Fetches a single discussion by its ID.
   * @param id The ID of the discussion to retrieve.
   * @returns An Observable of ApiResponse containing the DiscussionDTO.
   */
  getDiscussionById(id: number): Observable<ApiResponse<DiscussionDTO>> {
    return this.http.get<ApiResponse<DiscussionDTO>>(`${this.basePublicApiUrl}/${id}`)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log(`Discussion with id ${id} fetched successfully via service`, response.data);
          } else {
            console.error(`Failed to fetch discussion with id ${id} via service`, response.message, response.errors);
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
