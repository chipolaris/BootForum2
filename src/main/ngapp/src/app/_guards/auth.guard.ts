// src/app/_guards/auth.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthenticationService } from '../_services/authentication.service'; // Adjust path

export const authGuard: CanActivateFn = (route, state):
      Observable<boolean | UrlTree> | boolean | UrlTree => {

      const authService = inject(AuthenticationService);
      const router = inject(Router);

      // Check if token exists locally first (quick check)
      if (!authService.hasToken()) {
          console.log('AuthGuard: No token found, redirecting to login.');
          const returnUrl = state.url;
          return router.createUrlTree(['/app/login'], { queryParams: { returnUrl } }); // Adjust login path
      }

      // If token exists, verify it by fetching profile (more robust)
      return authService.getUserProfile().pipe(
        map(user => {
          if (user) {
            return true; // User profile fetched successfully, token is valid
          } else {
            // Profile fetch failed (likely invalid/expired token), redirect
            console.log('AuthGuard: Token validation failed (profile fetch), redirecting to login.');
            const returnUrl = state.url;
            return router.createUrlTree(['/app/login'], { queryParams: { returnUrl } }); // Adjust login path
          }
        }),
        catchError(() => {
          // Catch any unexpected errors during profile fetch
          console.log('AuthGuard: Error checking auth status, redirecting to login.');
          const returnUrl = state.url;
          return of(router.createUrlTree(['/app/login'], { queryParams: { returnUrl } })); // Adjust login path
        })
      );
};
