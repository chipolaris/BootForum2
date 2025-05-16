import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { ForumDTO, ForumCreateDTO, ForumUpdateDTO, ApiResponse } from '../_data/dtos'; // Ensure ApiResponse is imported

@Injectable({
  providedIn: 'root'
})
export class ForumService {
  private http = inject(HttpClient);
  // Base URL for forum operations, adjust if your admin path is different for get/update
  private baseAdminApiUrl = '/api/admin/forums';
  private createApiUrl = '/api/admin/create-forum'; // Kept for createForum

  createForum(payload: ForumCreateDTO): Observable<ApiResponse<ForumDTO>> {
    return this.http.post<ApiResponse<ForumDTO>>(this.createApiUrl, payload)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log('Forum created successfully via service', response.data);
          } else {
            console.error('Forum creation failed via service', response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  getForumById(id: number): Observable<ApiResponse<ForumDTO>> {
    return this.http.get<ApiResponse<ForumDTO>>(`${this.baseAdminApiUrl}/${id}`)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log(`Forum with id ${id} fetched successfully`, response.data);
          } else {
            console.error(`Failed to fetch forum with id ${id}`, response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  updateForum(id: number, payload: ForumUpdateDTO): Observable<ApiResponse<ForumDTO>> {
    return this.http.put<ApiResponse<ForumDTO>>(`${this.baseAdminApiUrl}/${id}`, payload)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log(`Forum with id ${id} updated successfully`, response.data);
          } else {
            console.error(`Failed to update forum with id ${id}`, response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  getAllForums(): Observable<ApiResponse<ForumDTO[]>> { // Expect an array of Forums
    return this.http.get<ApiResponse<ForumDTO[]>>(this.baseAdminApiUrl)
      .pipe(
        tap(response => {
          if (response.success) {
            console.log('All forums fetched successfully', response.data);
          } else {
            console.error('Failed to fetch all forums', response.message, response.errors);
          }
        }),
        catchError(this.handleError)
      );
  }

  private handleError(error: any): Observable<never> {
    console.error('An error occurred in ForumService:', error);
    let errorMessage = 'Something bad happened; please try again later.';
    if (error.error instanceof ErrorEvent) {
      // Client-side errors
      errorMessage = `Error: ${error.error.message}`;
    } else if (error.status) {
      // Server-side errors
      // Try to parse backend's ApiResponse if it's a structured error
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
