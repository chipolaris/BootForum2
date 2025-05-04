import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router'; // Import RouterModule for routerLink

@Component({
  selector: 'app-registration-confirmation',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './registration-confirmation.component.html',
  styleUrls: ['./registration-confirmation.component.css']
})
export class RegistrationConfirmationComponent {

  registrationKey: string | null = null;
  private router = inject(Router); // inject Router

  constructor() {
    // It's generally safer to access navigation state here or in ngOnInit
    // as getCurrentNavigation() might be null later in the lifecycle.
    const navigation = this.router.getCurrentNavigation();
    const state = navigation?.extras.state as {registrationKey: string}; // Type assertion

    if (state) {
      this.registrationKey = state.registrationKey;
      console.log('Received state:', state);
    } else {
      console.log('No navigation state received.');
      // Handle cases where state is missing (e.g., direct navigation, refresh)
    }
  }

  ngOnInit(): void {
    // You could also access the state here, but constructor is often preferred
    // for initial setup based on navigation.
    // Alternatively, use history.state if you don't want to inject Router,
    // but it's less type-safe:
    // const state = history.state;
    // if (state && state.email) { ... }
  }
}
