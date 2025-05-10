// src/app/_guards/auth.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators'; // Added tap for logging
import { AuthenticationService } from '../_services/authentication.service'; // Adjust path
import { User } from '../_data/models'; // Import User type for better type safety

export const authGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
): Observable<boolean | UrlTree> | boolean | UrlTree => {

  const authService = inject(AuthenticationService);
  const router = inject(Router);

  if (!authService.hasToken()) {
    console.log('AuthGuard: No token found locally, redirecting to login.');
    const returnUrl = state.url;
    return router.createUrlTree(['/app/login'], { queryParams: { returnUrl } });
  }

  console.log('AuthGuard: Token found. Calling authService.getUserProfile()');
  return authService.getUserProfile().pipe(
    tap(userFromService => { // Log what the service observable emits BEFORE map
      console.log('AuthGuard (tap before map): User object from service observable:',
        userFromService ? JSON.parse(JSON.stringify(userFromService)) : userFromService
      );
    }),
    map((user: User | null) => { // Explicitly type user
      console.log('AuthGuard (inside map): Processing user ->',
        user ? JSON.parse(JSON.stringify(user)) : user
      );

      if (user && typeof user === 'object' && user.username) { // More robust check for a valid user object
        console.log(`AuthGuard: User object is valid (username: ${user.username}).`);
        const requiredRoles = route.data?.['roles'] as Array<string> | undefined;
        console.log('AuthGuard: Required roles for this route:', requiredRoles);

        if (!requiredRoles || requiredRoles.length === 0) {
          console.log('AuthGuard: Route does not require specific roles. Access GRANTED.');
          return true;
        }

        // Access userRole with type safety if possible, otherwise fallback
        const userRoleFromProfile = user.userRole; // Directly access if User type has userRole
        // const userRoleFromProfile = (user as any).userRole; // Fallback if User type is generic

        console.log(`AuthGuard: Value of user.userRole from profile: '${userRoleFromProfile}' (type: ${typeof userRoleFromProfile})`);

        if (!userRoleFromProfile || typeof userRoleFromProfile !== 'string' || userRoleFromProfile.trim() === '') {
          console.log(`AuthGuard: User role is missing, not a string, or empty. Required: [${requiredRoles.join(', ')}]. Access DENIED.`);
          return router.createUrlTree(['/app/login'], { queryParams: { error: 'guard_user_role_invalid_or_missing' } });
        }

        const currentUserRoleUpper = userRoleFromProfile.toUpperCase();
        console.log(`AuthGuard: User role (uppercase): '${currentUserRoleUpper}'`);

        // Ensure requiredRoles are also compared in uppercase for consistency
        const hasPermission = requiredRoles.some(requiredRole =>
          typeof requiredRole === 'string' && currentUserRoleUpper === requiredRole.toUpperCase()
        );
        console.log(`AuthGuard: Checking permission: Does [${currentUserRoleUpper}] exist in [${requiredRoles.map(r => typeof r === 'string' ? r.toUpperCase() : r).join(', ')}]? Result: ${hasPermission}`);

        if (hasPermission) {
          console.log(`AuthGuard: User HAS permission. Access GRANTED.`);
          return true;
        } else {
          console.log(`AuthGuard: User does NOT have permission. Access DENIED.`);
          return router.createUrlTree(['/app/login'], { queryParams: { error: 'guard_unauthorized_access_role' } });
        }

      } else {
        console.log('AuthGuard: User object is null, undefined, or not a valid User structure after profile fetch. Access DENIED.');
        const returnUrl = state.url;
        return router.createUrlTree(['/app/login'], { queryParams: { returnUrl, error: 'guard_profile_fetch_returned_invalid_user' } });
      }
    }),
    catchError((error) => {
      console.error('AuthGuard: Unexpected error in getUserProfile pipe. Access DENIED.', error);
      const returnUrl = state.url;
      return of(router.createUrlTree(['/app/login'], { queryParams: { returnUrl, error: 'guard_pipe_error' } }));
    })
  );
};
