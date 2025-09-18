import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterModule, RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogModule } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { NgIf, CommonModule } from '@angular/common';

import { AuthenticationService } from './_services/authentication.service';
import { ConfigService } from './_services/config.service';
import { SystemStatisticService } from './_services/system-statistic.service';
import { UserDTO as User, SystemStatisticDTO, errorMessageFromApiResponse } from './_data/dtos';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterLink,
    RouterOutlet,
    RouterModule,
    ToastModule,
    ConfirmDialogModule,
    DynamicDialogModule,
    NgIf,
    CommonModule
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  siteName = 'BootForum2'; // default

  // --- Dependencies consistently using inject() ---
  private authService = inject(AuthenticationService);
  private router = inject(Router);
  private configService = inject(ConfigService);
  private systemStatisticService = inject(SystemStatisticService);

  // --- Public properties for the template ---
  isLoggedIn$: Observable<boolean>;
  isAdmin$: Observable<boolean>;
  systemStats: SystemStatisticDTO | null = null;
  statsError: string | null = null;
  isLoadingStats = true;
  isMobileMenuOpen = false; // For mobile navigation

  constructor() {
    // The constructor is now clean, handling only observable setup.
    this.isLoggedIn$ = this.authService.currentUser.pipe(map(user => !!user));
    this.isAdmin$ = this.authService.currentUser.pipe(
      map(user => !!(user?.userRoles?.includes('ADMIN')))
    );
  }

  ngOnInit(): void {
    // The APP_INITIALIZER now handles the initial auth state loading.
    console.log('AppComponent initialized. User authenticated:', this.authService.isAuthenticated());
    this.loadSystemStatistics();
    this.loadConfigSetting();
  }

  loadSystemStatistics(): void {
    this.isLoadingStats = true;
    this.statsError = null;
    this.systemStatisticService.getSystemStatistics().subscribe({
      next: (response) => {
        // Now handles the consistent ApiResponse wrapper
        if (response.success && response.data) {
          this.systemStats = response.data;
        } else {
          this.statsError = errorMessageFromApiResponse(response) || 'Failed to load system statistics.';
        }
        this.isLoadingStats = false;
      },
      // The error block is now unlikely to be hit due to the service's catchError,
      // but it's good practice to keep it for unexpected issues.
      error: (err) => {
        this.statsError = 'A critical error occurred. Please check the console.';
        this.isLoadingStats = false;
        console.error('Critical error in AppComponent statistics subscription:', err);
      }
    });
  }

  loadConfigSetting(): void {
    this.configService.getSettings(["general.siteName"]).subscribe({
      next: (data) => { // configService.getSettings() method handles the response level and expose the response.data
          if(data) {
            this.siteName = data.get("general.siteName");
          }
          // else this.siteName will keep default value of 'BootForum2'
      },
      error: (err) => {
        console.error('Error fetching config settings', err);
      }
    });
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  logout() {
    this.closeMobileMenu(); // Ensure menu closes on logout
    this.authService.logout();
    this.router.navigate(['/app/login']);
  }
}
