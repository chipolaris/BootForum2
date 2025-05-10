import { Component, Inject, OnInit } from '@angular/core';
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
  providers: [MessageService]
})
export class AppComponent implements OnInit {
  title = 'BootForum2';

  isLoggedIn$: Observable<boolean>;
  isAdmin$: Observable<boolean>;

  constructor(
     public authService: AuthenticationService, // @Inject is optional for public constructor params
     public router: Router
  ) {
     this.isLoggedIn$ = this.authService.currentUser.pipe(map(user => !!user));
     this.isAdmin$ = this.authService.currentUser.pipe(
       map((user: User | null) => !!(user && user.userRole === 'ADMIN'))
     );
  }

  ngOnInit(): void {
    if (this.authService.hasToken() && !this.authService.isAuthenticated()) {
       this.authService.getUserProfile().subscribe({
        error: (err) => {
          console.error('AppComponent: Failed to re-establish session, logging out.', err);
          this.authService.logout();
        }
       });
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/app/login']); // Redirect to login screen after logout
  }
}
