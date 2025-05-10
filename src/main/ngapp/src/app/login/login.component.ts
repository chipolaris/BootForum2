import { Component, OnInit } from '@angular/core';
import { Router, RouterModule, ActivatedRoute } from '@angular/router'; // Import RouterModule if using routerLink
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule, // Import ReactiveFormsModule
} from '@angular/forms';
import { CommonModule } from '@angular/common'; // Import CommonModule for *ngIf etc.
import { finalize } from 'rxjs/operators';

import { NgIcon, provideIcons } from '@ng-icons/core';
import { heroArrowRightEndOnRectangle, heroLockClosed } from '@ng-icons/heroicons/outline';

// --- PrimeNG Modules ---
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

// --- Assume you have an AuthService like this ---
import { AuthenticationService } from '../_services/authentication.service'; // Adjust path as needed
// ---------------------------------------------


@Component({
  selector: 'app-login',
  standalone: true, // Make this component standalone
  imports: [
    // --- Import necessary modules directly ---
    CommonModule,
    ReactiveFormsModule,
    RouterModule, // Needed if you add routerLink for register/reset password
    ToastModule,
    InputTextModule,
    ProgressSpinnerModule,
    NgIcon,
    // -----------------------------------------
  ],
  providers: [
    // MessageService is often provided root, but can be provided here if needed
    // If AuthService is not providedIn: 'root', provide it here:
    // AuthService
    AuthenticationService, MessageService, provideIcons({heroArrowRightEndOnRectangle, heroLockClosed})
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup; // Definite assignment assertion
  isLoading = false;
  submitted = false;
  loginError: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthenticationService, // Inject your AuthService
    private router: Router,
    private route: ActivatedRoute, // Inject ActivatedRoute
    private messageService: MessageService // Inject PrimeNG MessageService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  /**
   * Initializes the login form with validation rules.
   */
  private initializeForm(): void {
    this.loginForm = this.fb.group({
      // Assuming username login, use 'email' if needed and add Validators.email
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  // Convenience getter for easy access to form fields in the template
  get f() {
    return this.loginForm.controls;
  }

  /**
   * Handles the login form submission.
   */
  onSubmit(): void {
    this.submitted = true;

    // Stop here if form is invalid
    if (this.loginForm.invalid) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Validation Error',
        detail: 'Please enter username and password.',
      });
      return;
    }

    this.isLoading = true;

    const username = this.f['username'].value;
    const password = this.f['password'].value;

    // Call the AuthService to perform login
    this.authService
      .login(username, password)
      .pipe(
        // Ensure isLoading is set to false regardless of success/error
        finalize(() => (this.isLoading = false))
      )
      .subscribe({
        next: (user) => {
          // Login successful
          this.messageService.add({
            severity: 'success',
            summary: 'Login Successful',
            detail: `Welcome back!`,
          }); // Customize user info if available
          // Navigate to a protected route (e.g., dashboard)
          // --- HANDLE returnUrl ---
          const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/app/dashboard';
          this.router.navigateByUrl(returnUrl);
          // ------------------------------------
        },
        error: (error) => {
          // Login failed - error should be a user-friendly message from AuthService
          console.error('Login failed:', error);
          this.loginError = 'Login failed';
          this.messageService.add({
            severity: 'error',
            summary: 'Login Failed',
            detail: error || 'Invalid username or password.',
          });
          this.submitted = false; // Allow retry without page reload
        },
      });
  }
}
