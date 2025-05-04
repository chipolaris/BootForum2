    // src/app/_services/authentication.service.ts
    import { Injectable } from '@angular/core';
    import { HttpClient, HttpHeaders } from '@angular/common/http';
    import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
    import { map, catchError, tap, shareReplay, switchMap } from 'rxjs/operators';
    import { User } from '../_data/models';

    // Define the expected response structure from /api/authenticate
    interface AuthResponse {
      accessToken: string;
      tokenType: string; // Usually 'Bearer'
    }

    @Injectable({ providedIn: 'root' })
    export class AuthenticationService {
        private currentUserSubject: BehaviorSubject<User | null>;
        public currentUser: Observable<User | null>;
        private profileRequest$: Observable<User | null> | null = null;

        // API Endpoints
        private authUrl = '/api/authenticate'; // Changed from /api/login
        private profileUrl = '/api/user/profile';
        private readonly TOKEN_KEY = 'authToken'; // Key for localStorage

        constructor(private http: HttpClient) {
            // Initialize state - try to load user based on stored token validity
            this.currentUserSubject = new BehaviorSubject<User | null>(null);
            this.currentUser = this.currentUserSubject.asObservable();
            // Optionally trigger initial check if a token exists
            if (this.getCurrentUserToken()) {
                 this.getUserProfile().subscribe(); // Attempt to fetch profile to validate token
            }
        }

        public get currentUserValue(): User | null {
            return this.currentUserSubject.value;
        }

        /**
         * Retrieves the stored JWT token from localStorage.
         */
        public getCurrentUserToken(): string | null {
            return localStorage.getItem(this.TOKEN_KEY);
        }

        /**
         * Checks if a token exists locally. For a more robust check,
         * combine this with getUserProfile() or token expiration check.
         */
        public hasToken(): boolean {
            return !!this.getCurrentUserToken();
        }

        /**
         * Attempts to fetch the user profile from the backend using the JWT.
         * This implicitly validates the token on the backend.
         */
        getUserProfile(): Observable<User | null> {
            // If no token, don't bother making the request
            if (!this.hasToken()) {
                this.currentUserSubject.next(null);
                this.profileRequest$ = null; // Clear cache if any
                return of(null);
            }

            // Use caching if desired
            if (this.profileRequest$) {
                return this.profileRequest$;
            }

            this.profileRequest$ = this.http.get<User>(this.profileUrl).pipe(
                tap(user => {
                    console.log("User profile fetched:", user);
                    this.currentUserSubject.next(user); // Update state on success
                }),
                catchError(error => {
                    console.error("Failed to fetch user profile (token might be invalid/expired):", error.status);
                    this.logout(); // Log out if profile fetch fails (e.g., 401)
                    return of(null); // Return null on error
                }),
                shareReplay(1)
            );
            return this.profileRequest$;
        }

        /**
         * Logs in the user by POSTing credentials as JSON.
         * Stores the received JWT in localStorage.
         */
        login(username: string, password: string): Observable<User | null> { // Return User | null
            // Clear previous state/token before attempting login
            this.removeToken();
            this.profileRequest$ = null;
            this.currentUserSubject.next(null);

            return this.http.post<AuthResponse>(this.authUrl, { username, password }) // Send JSON
                .pipe(
                    tap(response => {
                        // Store the received token
                        if (response && response.accessToken) {
                            this.storeToken(response.accessToken);
                            console.log("Token stored successfully.");
                        } else {
                            console.error("Login response did not contain an access token.");
                            throw new Error("Invalid login response from server.");
                        }
                    }),
                    // After storing token, fetch profile to get user details & confirm token works
                    switchMap(() => this.getUserProfile()),
                    catchError(error => {
                        console.error("Login failed:", error);
                        this.removeToken(); // Ensure token is removed on login failure
                        // Rethrow a user-friendly error
                        return throwError(() => new Error('Invalid username or password.'));
                    })
                );
        }

        /**
         * Logs out the user by removing the token from localStorage.
         */
        logout(): void {
            this.removeToken();
            this.currentUserSubject.next(null);
            this.profileRequest$ = null; // Clear profile cache
            console.log("User logged out (token removed).");
            // No backend call needed for simple JWT logout unless implementing blocklisting
        }

        private storeToken(token: string): void {
            localStorage.setItem(this.TOKEN_KEY, token);
        }

        private removeToken(): void {
            localStorage.removeItem(this.TOKEN_KEY);
        }
    }
