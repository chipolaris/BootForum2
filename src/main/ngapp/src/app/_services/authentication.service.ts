import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { map, catchError, tap, shareReplay, switchMap, first } from 'rxjs/operators'; // Added first

import { UserDTO } from '../_data/dtos';

interface AuthResponse {
  accessToken: string;
  tokenType: string;
}

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
	private currentUserSubject: BehaviorSubject<UserDTO | null>;
	public currentUser: Observable<UserDTO | null>;
	// Remove profileRequest$ caching from here if APP_INITIALIZER handles the first call
	// private profileRequest$: Observable<UserDTO | null> | null = null;

	private authUrl = '/api/authenticate';
	private profileUrl = '/api/user/profile';
	private readonly TOKEN_KEY = 'authToken';

	constructor(private http: HttpClient) {
		this.currentUserSubject = new BehaviorSubject<UserDTO | null>(null);
		this.currentUser = this.currentUserSubject.asObservable();
		// We will move the initial profile fetch to an explicit init method
		// for APP_INITIALIZER
	}

	// NEW METHOD: To be called by APP_INITIALIZER
	public initializeAuthState(): Observable<UserDTO | null> {
		const token = this.getCurrentUserToken();
		if (token) {
			// Fetch profile and ensure the observable completes for APP_INITIALIZER
			return this.http.get<UserDTO>(this.profileUrl).pipe(
				tap(user => {
					console.log("User profile fetched during init:", user);
					this.currentUserSubject.next(user);
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

	// Modified getUserProfile to not rely on its own caching if APP_INITIALIZER handles first call
	getUserProfile(): Observable<UserDTO | null> {
		if (!this.hasToken()) {
			this.currentUserSubject.next(null);
			return of(null);
		}
		// If already authenticated, return current user to avoid redundant calls
		// This can happen if called after APP_INITIALIZER or during navigation
		if (this.isAuthenticated()) {
			return of(this.currentUserValue);
		}

		return this.http.get<UserDTO>(this.profileUrl).pipe(
			tap(user => {
				console.log("User profile fetched:", user);
				this.currentUserSubject.next(user);
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
				switchMap(() => this.getUserProfile()), // Fetch profile after login
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
}
