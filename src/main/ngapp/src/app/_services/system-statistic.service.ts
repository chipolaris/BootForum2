import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiResponse, SystemStatisticDTO } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class SystemStatisticService {
  private http = inject(HttpClient);
  private apiUrl = '/api/public/system-statistic';

  /**
   * Fetches system statistics and wraps the raw DTO in a standard ApiResponse
   * to ensure a consistent return type across all services.
   */
  getSystemStatistics(): Observable<ApiResponse<SystemStatisticDTO>> {
    return this.http.get<SystemStatisticDTO>(this.apiUrl).pipe(
      map(data => ({
        success: true,
        data: data,
        timestamp: new Date().toISOString(),
        message: null,
        errors: null
      })),
      catchError((err: HttpErrorResponse) => {
        console.error('An error occurred in SystemStatisticService:', err);

        // FIXED: The error response must also match the function's return type.
        // Since the `data` property in ApiResponse<T> can be null, this is valid.
        const errorResponse: ApiResponse<SystemStatisticDTO> = {
          success: false,
          message: 'Could not connect to the server or an unexpected error occurred.',
          errors: [err.message],
          timestamp: new Date().toISOString(),
          data: null
        };
        // Return a new observable of the error response so the component's `next` block can handle it
        return of(errorResponse);
      })
    );
  }
}
