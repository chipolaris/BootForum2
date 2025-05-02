import { Component, Inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { NgIf, CommonModule } from '@angular/common';

import { AuthenticationService } from './_services/authentication.service';

@Component({
  selector: 'app-root',
  imports: [ RouterLink, RouterOutlet, ToastModule, NgIf, CommonModule ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  providers: [MessageService]
})
export class AppComponent {
  title = 'Bootgular Machine Learning';

  isLoggedIn$: Observable<boolean>;

  constructor(@Inject(AuthenticationService) public authService: AuthenticationService, @Inject(Router) public router: Router) { // Make public or use getter
     this.isLoggedIn$ = this.authService.currentUser.pipe(map(user => !!user));
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/app/login']); // Redirect to login screen after logout
  }
}
