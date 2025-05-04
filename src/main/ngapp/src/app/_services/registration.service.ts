import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { RegistrationPayload, ApiResponse } from '../_data/dtos';

@Injectable({ providedIn: 'root' })
export class RegistrationService {
  private http = inject(HttpClient);

  private registrationUrl = '/api/public/register'; // registration API URL
  private emailConfirmationUrl = '/api/public/confirm-registration-email'; // email confirmation API URL

  /**
   * Confirms user registration via email link key.
   * @param registrationKey The unique key from the confirmation link.
   * @returns Observable<ApiResponse<ConfirmationSuccessData | null>>
   * Note: Using `any` temporarily because success/error structures differ significantly in `data`/`errors`.
   * A more robust solution might involve distinct response types or a union type.
   */
  confirmEmail(registrationKey: string): Observable<ApiResponse<any>> {
    const params = new HttpParams().set('registrationKey', registrationKey);
    const url = `${this.emailConfirmationUrl}`;

    // POST request with query parameters, no body needed for this endpoint
    return this.http.post<ApiResponse<any>>(url, null, { params })
      .pipe(
        tap(response => console.log('Email confirmation response:', response)),
        catchError(this.handleError)
      );
  }

  /**
   * Registers a new user.
   * @param userData The user registration data.
   * @returns Observable<ApiResponse<string>>
   */
  register(userData: RegistrationPayload): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.registrationUrl}`, userData)
      .pipe(
        tap(response => console.log('Registration submitted:', response)), // Log success
        catchError(this.handleError) // Use a shared error handler
      );
  }

  // Example shared error handler (or keep it specific to register)
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unknown error occurred!';
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Error: ${error.error.message}`;
    }
    else if(Array.isArray(error.error)) {
      errorMessage = error.error.join('; ');
    }
    else {
      // Backend error
      errorMessage = `Error Code: ${error.status}\nMessage: ${error.message}`;
      // Try to get message from backend response body
      if (error.error && error.error.message) {
        errorMessage = error.error.message;
      }
    }
    console.error('RegistrationService Error:', errorMessage, error);
    return throwError(() => new Error(errorMessage)); // Return an Error object
  }
}
