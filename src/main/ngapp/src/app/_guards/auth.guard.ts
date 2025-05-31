import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthenticationService } from '../_services/authentication.service';
import { map, take } from 'rxjs/operators';
import { of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthenticationService);
  const router = inject(Router);

  // If APP_INITIALIZER has run, isAuthenticated should be reliable
  if (authService.isAuthenticated()) {
	// Check roles if necessary
	const expectedRoles = route.data['roles'] as Array<string>;
	if (expectedRoles && expectedRoles.length > 0) { // Ensure expectedRoles is not empty
	  const currentUser = authService.currentUserValue;
	  // Check if currentUser exists and has userRoles
	  if (currentUser && currentUser.userRoles && currentUser.userRoles.length > 0) {
		// Check if any of the user's roles are present in the expectedRoles array
		const hasRequiredRole = currentUser.userRoles.some(role => expectedRoles.includes(role));
		if (hasRequiredRole) {
		  return true;
		} else {
		  console.warn(`AuthGuard: User with roles [${currentUser.userRoles.join(', ')}] does not have any of the required roles [${expectedRoles.join(', ')}].`);
		  router.navigate(['/app/unauthorized']); // Or some other appropriate page
		  return false;
		}
	  } else {
		// User is authenticated but has no roles or userRoles property is missing/empty
		console.warn(`AuthGuard: User is authenticated but has no roles assigned or roles are not available. Required roles: [${expectedRoles.join(', ')}].`);
		router.navigate(['/app/unauthorized']);
		return false;
	  }
	}
	// No specific roles required for this route, or expectedRoles is empty
	return true;
  }

  // If not authenticated by now (after APP_INITIALIZER), redirect to login
  console.log('AuthGuard: User not authenticated, redirecting to login.');
  router.navigate(['/app/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
