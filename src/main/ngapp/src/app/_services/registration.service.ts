import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

// Define an interface for the registration payload (matches SignUpRequest without confirmPassword)
export interface RegistrationPayload {
  username: string;
  password?: string; // Password might be optional if handled differently, but usually required
  firstName: string;
  lastName: string;
  email: string;
}

// Define an interface for the backend's success/error message response
export interface MessageResponse {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class RegistrationService {
  private http = inject(HttpClient);
  // Adjust base URL if needed
  private registrationUrl = '/api/public/register'; // Your base API URL

  /**
   * Registers a new user.
   * @param userData The user registration data.
   * @returns Observable<MessageResponse>
   */
  register(userData: RegistrationPayload): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.registrationUrl}`, userData)
      .pipe(
        tap(response => console.log('Registration API success:', response)), // Log success
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
      errorMessage = error.error.join(', ');
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
