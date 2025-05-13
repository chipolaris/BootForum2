import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ForumGroupDTO, ApiResponse } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class ForumGroupService {
  private http = inject(HttpClient);
  private baseAdminApiUrl = '/api/admin/forum-groups';
  private createApiUrl = '/api/admin/create-forum-group';
  private rootForumGroupApiUrl = '/api/admin/root-forum-group'; // New URL

  createForumGroup(payload: ForumGroupDTO): Observable<ApiResponse<ForumGroupDTO>> {
    return this.http.post<ApiResponse<ForumGroupDTO>>(this.createApiUrl, payload)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log('Forum Group created successfully via service', response.data);
          } else {
            console.error('Forum Group creation failed via service', response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  getForumGroupById(id: number): Observable<ApiResponse<ForumGroupDTO>> {
    return this.http.get<ApiResponse<ForumGroupDTO>>(`${this.baseAdminApiUrl}/${id}`)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log(`Forum Group with id ${id} fetched successfully`, response.data);
          } else {
            console.error(`Failed to fetch forum group with id ${id}`, response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  updateForumGroup(id: number, payload: ForumGroupDTO): Observable<ApiResponse<ForumGroupDTO>> {
    // Note: Backend controller currently returns ApiResponse<ForumGroup> (domain object) on update.
    // Ideally, it should return ApiResponse<ForumGroupDTO>.
    // This frontend service is typed to expect ApiResponse<ForumGroupDTO>.
    // If backend sends a different structure, adjustments might be needed here or in component.
    return this.http.put<ApiResponse<ForumGroupDTO>>(`${this.baseAdminApiUrl}/${id}`, payload)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log(`Forum Group with id ${id} updated successfully`, response.data);
          } else {
            console.error(`Failed to update forum group with id ${id}`, response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  // <<< NEW METHOD: Get Root Forum Group >>>
  getRootForumGroup(): Observable<ApiResponse<ForumGroupDTO>> {
    return this.http.get<ApiResponse<ForumGroupDTO>>(this.rootForumGroupApiUrl)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log('Root Forum Group fetched successfully', response.data);
          } else {
            console.error('Failed to fetch root forum group', response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  private handleError(error: any): Observable<never> {
    console.error('An error occurred in ForumGroupService:', error);
    let errorMessage = 'Something bad happened; please try again later.';
    if (error.error instanceof ErrorEvent) {
      errorMessage = `Error: ${error.error.message}`;
    } else if (error.status) {
      if (error.error && typeof error.error.message === 'string') {
        errorMessage = `Error ${error.status}: ${error.error.message}`;
        if(error.error.errors && Array.isArray(error.error.errors)) {
          errorMessage += ` Details: ${error.error.errors.join(', ')}`;
        }
      } else {
        errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
      }
    }
    return throwError(() => new Error(errorMessage));
  }
}
