import { Component, OnInit, inject } from '@angular/core'; // Added inject
import { Router, RouterLink, RouterModule, RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
// MessageService is provided in app.config.ts, so no need to import here unless used directly in this component's template
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { NgIf, CommonModule } from '@angular/common'; // CommonModule provides DatePipe if needed in template

import { AuthenticationService } from './_services/authentication.service';
import { SystemStatisticService } from './_services/system-statistic.service'; // Import the new service
import { UserDTO as User, SystemStatisticDTO } from './_data/dtos'; // Import the DTO

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ RouterLink, RouterOutlet, RouterModule, ToastModule, NgIf, CommonModule ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  providers: [ ]
})
export class AppComponent implements OnInit {
  title = 'BootForum2';

  isLoggedIn$: Observable<boolean>;
  isAdmin$: Observable<boolean>;

  // Properties for System Statistics
  systemStats: SystemStatisticDTO | null = null;
  statsError: string | null = null;
  isLoadingStats = true;

  // Inject SystemStatisticService
  private systemStatisticService = inject(SystemStatisticService);

  constructor(
	 public authService: AuthenticationService,
	 public router: Router
  ) {
	 this.isLoggedIn$ = this.authService.currentUser.pipe(map(user => !!user));
	 this.isAdmin$ = this.authService.currentUser.pipe(
     map((user: User | null) => {
       return !!(user && user.userRoles && user.userRoles.includes('ADMIN'));
     })
   );
  }

  ngOnInit(): void {
    // The APP_INITIALIZER now handles the initial auth state loading.
    console.log('AppComponent initialized. User authenticated:', this.authService.isAuthenticated());
    this.loadSystemStatistics(); // Load system stats on init
  }

  loadSystemStatistics(): void {
    this.isLoadingStats = true;
    this.statsError = null;
    this.systemStatisticService.getSystemStatistics().subscribe({
      next: (data) => {
        this.systemStats = data;
        this.isLoadingStats = false;
      },
      error: (err) => {
        this.statsError = err.message || 'Failed to load system statistics.';
        this.isLoadingStats = false;
        console.error('Error loading system statistics in AppComponent:', err);
      }
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/app/login']);
  }
}
