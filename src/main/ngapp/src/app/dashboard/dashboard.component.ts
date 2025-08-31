import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthenticationService } from '../_services/authentication.service';
import { UserDTO } from '../_data/dtos';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { APP_ICONS } from '../shared/hero-icons';

@Component({
  selector: 'app-dashboard',
  standalone: true, // Make it a standalone component
  imports: [CommonModule, RouterModule, NgIcon], // Import modules for routing, icons, and directives
  providers: [provideIcons(APP_ICONS)], // Provide the application's icon set
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private authService = inject(AuthenticationService);
  currentUser: UserDTO | null = null;

  ngOnInit(): void {
    // Get the currently logged-in user from the authentication service
    this.currentUser = this.authService.currentUserValue;
  }
}
