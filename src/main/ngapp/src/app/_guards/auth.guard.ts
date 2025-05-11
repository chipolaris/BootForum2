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
	if (expectedRoles) {
	  const currentUser = authService.currentUserValue;
	  if (currentUser && expectedRoles.some(role => currentUser.userRole === role)) {
		return true;
	  } else {
		console.warn('AuthGuard: User does not have required role.');
		router.navigate(['/app/unauthorized']); // Or some other appropriate page
		return false;
	  }
	}
	return true;
  }

  // If not authenticated by now (after APP_INITIALIZER), redirect to login
  console.log('AuthGuard: User not authenticated, redirecting to login.');
  router.navigate(['/app/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
