import { Component, OnInit } from '@angular/core'; // Removed Inject
import { Router, RouterLink, RouterModule, RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { NgIf, CommonModule } from '@angular/common';

import { AuthenticationService } from './_services/authentication.service';
import { User } from './_data/models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ RouterLink, RouterOutlet, RouterModule, ToastModule, NgIf, CommonModule ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  providers: [ ] // MessageService is usually provided in app.config
})
export class AppComponent implements OnInit { // Removed 'implements OnInit' if ngOnInit is empty
  title = 'BootForum2';

  isLoggedIn$: Observable<boolean>;
  isAdmin$: Observable<boolean>;

  constructor(
	 public authService: AuthenticationService,
	 public router: Router
  ) {
	 this.isLoggedIn$ = this.authService.currentUser.pipe(map(user => !!user));
	 this.isAdmin$ = this.authService.currentUser.pipe(
     map((user: User | null) => {
       // Check if user exists, has userRoles array, and if 'ADMIN' is one of the roles
       return !!(user && user.userRoles && user.userRoles.includes('ADMIN'));
     })
   );
  }

  ngOnInit(): void {
    // The APP_INITIALIZER now handles the initial auth state loading.
    // This logic can be removed or simplified.
    // You might still want to log if the user is authenticated after init.
    console.log('AppComponent initialized. User authenticated:', this.authService.isAuthenticated());
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/app/login']);
  }
}
