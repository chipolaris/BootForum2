import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ApiResponse, DiscussionDTO, DiscussionInfoDTO, DiscussionSummaryDTO, Page } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class DiscussionService {
  private http = inject(HttpClient);
  private baseUserApiUrl = '/api/user/discussions';
  private basePublicApiUrl = '/api/public/discussions';

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
   * Fetches a paginated and sorted list of discussion summaries.
   * If a forumId is provided, it fetches discussions for that specific forum.
   * Otherwise, it fetches all discussions in the system.
   *
   * @param forumId Optional ID of the forum to filter discussions.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of discussions per page.
   * @param sortProperty The property to sort by.
   * @param sortDirection The direction of sorting ('ASC' or 'DESC').
   * @returns An Observable of ApiResponse containing a Page of DiscussionSummaryDTOs.
   */
  listDiscussionSummaries(
    page: number = 0,
    size: number = 20, // Default size
    sortProperty: string = 'createDate', // Default sort
    sortDirection: string = 'DESC' // Default direction
  ): Observable<ApiResponse<Page<DiscussionSummaryDTO>>> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', `${sortProperty},${sortDirection}`);

    // Determine the correct endpoint based on the presence of forumId
    const url = `${this.basePublicApiUrl}/list`;

    return this.http.get<ApiResponse<Page<DiscussionSummaryDTO>>>(url, { params })
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
   * Fetches a paginated and sorted list of discussions.
   * @param forumId Optional ID of the forum to filter discussions.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of discussions per page.
   * @param sortProperty The property to sort by.
   * @param sortDirection The direction of sorting ('ASC' or 'DESC').
   * @returns An Observable of ApiResponse containing a Page of DiscussionDTOs.
   */
  listDiscussionSummariesByForum(
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

    const discussionsUrl = `${this.basePublicApiUrl}/comments/by-forum/${forumId}`;

    return this.http.get<ApiResponse<Page<DiscussionSummaryDTO>>>(discussionsUrl, { params })
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

  /**
   * Searches for discussions based on a keyword.
   * @param keyword The search term.
   * @param page The page number to retrieve (0-indexed).
   * @param size The number of discussions per page.
   * @returns An Observable of ApiResponse containing a Page of DiscussionInfoDTOs.
   */
  searchDiscussions(
    keyword: string,
    page: number = 0,
    size: number = 10
  ): Observable<ApiResponse<Page<DiscussionInfoDTO>>> {
    const params = new HttpParams()
      .set('keyword', keyword)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<ApiResponse<Page<DiscussionInfoDTO>>>(`${this.basePublicApiUrl}/search`, { params })
      .pipe(
        tap(response => {
          if (response.success) {
            console.log('Discussions searched successfully via service', response.data);
          } else {
            console.error('Failed to search discussions via service', response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  /**
   * Fetches the 5 most recent discussions.
   */
  getLatestDiscussions(): Observable<ApiResponse<DiscussionDTO[]>> {
    return this.http.get<ApiResponse<DiscussionDTO[]>>(`${this.basePublicApiUrl}/latest`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Fetches the 5 most commented-on discussions.
   */
  getMostCommentedDiscussions(): Observable<ApiResponse<DiscussionDTO[]>> {
    return this.http.get<ApiResponse<DiscussionDTO[]>>(`${this.basePublicApiUrl}/most-commented`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Fetches the 5 most viewed discussions.
   */
  getMostViewedDiscussions(): Observable<ApiResponse<DiscussionDTO[]>> {
    return this.http.get<ApiResponse<DiscussionDTO[]>>(`${this.basePublicApiUrl}/most-viewed`)
      .pipe(catchError(this.handleError));
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
