import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { map, catchError, tap, shareReplay, switchMap, first } from 'rxjs/operators';

import { UserDTO, ApiResponse } from '../_data/dtos'; // Make sure ApiResponse is imported

interface AuthResponse {
  accessToken: string;
  tokenType: string;
}

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
	private currentUserSubject: BehaviorSubject<UserDTO | null>;
	public currentUser: Observable<UserDTO | null>;

	private authUrl = '/api/authenticate';
	private profileUrl = '/api/user/my-profile';
	private readonly TOKEN_KEY = 'authToken';

	constructor(private http: HttpClient) {
		this.currentUserSubject = new BehaviorSubject<UserDTO | null>(null);
		this.currentUser = this.currentUserSubject.asObservable();
	}

	// Corrected initializeAuthState method
	public initializeAuthState(): Observable<UserDTO | null> {
		const token = this.getCurrentUserToken();
		if (token) {
			// Fetch profile and ensure the observable completes for APP_INITIALIZER
			return this.http.get<ApiResponse<UserDTO>>(this.profileUrl).pipe( // 1. Correctly type the response
				map(response => { // 2. Use map to extract the data
					if (response.success && response.data) {
						console.log("User profile fetched during init:", response.data);
						this.currentUserSubject.next(response.data); // 3. Store the correct UserDTO object
						return response.data;
					}
					// If API call is successful but data is missing, treat as an error
					this.logout();
					return null;
				}),
				catchError(error => {
					console.error("Failed to fetch user profile during init (token might be invalid/expired):", error.status);
					this.logout(); // Important: clear invalid token
					return of(null); // Must return an observable that completes
				}),
				first() // Ensures the observable completes after the first emission
			);
		} else {
			return of(null); // No token, resolve immediately
		}
	}

	public get currentUserValue(): UserDTO | null {
		return this.currentUserSubject.value;
	}

	public getCurrentUserToken(): string | null {
		return localStorage.getItem(this.TOKEN_KEY);
	}

	public hasToken(): boolean {
		return !!this.getCurrentUserToken();
	}

	public isAuthenticated(): boolean {
		return !!this.currentUserSubject.value;
	}

	// Corrected getUserProfile method
	getUserProfile(): Observable<UserDTO | null> {
		if (!this.hasToken()) {
			this.currentUserSubject.next(null);
			return of(null);
		}
		// If already authenticated, return current user to avoid redundant calls
		if (this.isAuthenticated()) {
			return of(this.currentUserValue);
		}

		return this.http.get<ApiResponse<UserDTO>>(this.profileUrl).pipe( // 1. Correctly type the response
			map(response => { // 2. Use map to extract the data
				if (response.success && response.data) {
					console.log("User profile fetched:", response.data);
					this.currentUserSubject.next(response.data); // 3. Store the correct UserDTO object
					return response.data;
				}
				this.logout();
				return null;
			}),
			catchError(error => {
				console.error("Failed to fetch user profile (token might be invalid/expired):", error.status);
				this.logout();
				return of(null);
			}),
			shareReplay(1) // Keep shareReplay for subsequent calls
		);
	}

	login(username: string, password: string): Observable<UserDTO | null> {
		this.removeToken();
		this.currentUserSubject.next(null);

		return this.http.post<AuthResponse>(this.authUrl, { username, password })
			.pipe(
				tap(response => {
					if (response && response.accessToken) {
						this.storeToken(response.accessToken);
					} else {
						throw new Error("Invalid login response from server.");
					}
				}),
				switchMap(() => this.getUserProfile()), // This now calls the corrected getUserProfile
				catchError(error => {
					console.error("Login failed:", error);
					this.removeToken();
					return throwError(() => new Error('Invalid username or password.'));
				})
			);
	}

	logout(): void {
		this.removeToken();
		this.currentUserSubject.next(null);
		console.log("User logged out (token removed).");
	}

	private storeToken(token: string): void {
		localStorage.setItem(this.TOKEN_KEY, token);
	}

	private removeToken(): void {
		localStorage.removeItem(this.TOKEN_KEY);
	}

	/**
	 * Updates the current user's state within the application.
	 * This is useful after a profile update to ensure all components
	 * subscribed to the current user receive the latest data without a full refresh.
	 * @param updatedUser The complete, updated UserDTO object.
	 */
	public updateCurrentUser(updatedUser: UserDTO): void {
		const currentUser = this.currentUserValue;
		// Ensure we are updating the state for the currently logged-in user.
		if (currentUser && updatedUser && currentUser.username === updatedUser.username) {
			this.currentUserSubject.next(updatedUser);
			console.log("Current user state updated in AuthenticationService:", updatedUser);
		} else {
			console.warn("Attempted to update current user with mismatched data or when no user is logged in.");
		}
	}
}
