import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { SystemStatisticDTO } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class SystemStatisticService {
  private http = inject(HttpClient);
  private apiUrl = '/api/public/system-statistic'; // Matches your SystemStatisticController

  constructor() { }

  getSystemStatistics(): Observable<SystemStatisticDTO> {
    return this.http.get<SystemStatisticDTO>(this.apiUrl)
      .pipe(
        tap(data => console.log('System statistics fetched:', data)),
        catchError(this.handleError)
      );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('An error occurred in SystemStatisticService:', error);
    let errorMessage = 'Something bad happened while fetching system statistics; please try again later.';

    if (error.error instanceof ErrorEvent) {
      // Client-side or network error
      errorMessage = `An error occurred: ${error.error.message}`;
    } else if (error.status === 0) {
      errorMessage = 'Could not connect to the server. Please check your network connection.';
    } else if (error.error && typeof error.error === 'string') {
      // If backend returns a simple string error
      errorMessage = `Error ${error.status}: ${error.error}`;
    } else if (error.message) {
      errorMessage = `Error ${error.status}: ${error.message}`;
    }
    return throwError(() => new Error(errorMessage));
  }
}
