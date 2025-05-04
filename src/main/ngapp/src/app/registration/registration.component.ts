import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { RegistrationService } from '../_services/registration.service';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

// Custom Validator for Password Match
export function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');

  // Don't validate if controls aren't present or haven't been touched yet
  if (!password || !confirmPassword || !password.value || !confirmPassword.value) {
    return null;
  }

  return password.value === confirmPassword.value ? null : { passwordMismatch: true };
}


@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule, // For routerLink
    ToastModule,
    ProgressSpinnerModule
  ],
  providers: [MessageService], // Provide if not global
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css'] // Can be empty if only using Tailwind
})
export class RegistrationComponent implements OnInit {

  registrationForm!: FormGroup;
  isLoading = false;
  submitted = false;
  generalErrors: string[] | null = null;

  private fb = inject(FormBuilder);
  private registrationService = inject(RegistrationService);
  private router = inject(Router);
  private messageService = inject(MessageService);

  ngOnInit(): void {
    this.registrationForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(20)]],
      password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(40)]],
      confirmPassword: ['', Validators.required],
      firstName: ['', [Validators.required, Validators.maxLength(50)]],
      lastName: ['', [Validators.required, Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(50)]]
    }, { validators: passwordMatchValidator }); // Add custom validator to the group
  }

  // Convenience getter for easy access to form fields
  get f() { return this.registrationForm.controls; }
  get formErrors() { return this.registrationForm.errors; }

  onSubmit(): void {
    this.submitted = true;
    this.generalErrors = null; // Clear previous errors

    // Stop here if form is invalid
    if (this.registrationForm.invalid) {
      console.log('Registration form invalid:', this.registrationForm.errors);
      this.messageService.add({ severity: 'warn', summary: 'Validation Error', detail: 'Please check the form for errors.' });
      return;
    }

    this.isLoading = true;

    // Prepare data - exclude confirmPassword
    const { confirmPassword, ...registrationData } = this.registrationForm.value;

    this.registrationService.register(registrationData)
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (response) => {
          if (response.success && response.data) {
            console.log('Registration successful:\n', response);
            this.messageService.add({ severity: 'success', summary: 'Registration Successful', detail: response.message || 'Registration submitted!' });

            this.router.navigate(['/app/registration-confirmation'], { state: { registrationKey: response.data } });
          }
          else if(response.errors){
            // Handle errors
            this.generalErrors = response.errors;

            console.error('Registration failed:\n', this.generalErrors.join('\n'));
            this.messageService.add({ severity: 'error', summary: 'Registration Failed', detail: this.generalErrors.join('; ') ?? 'Please try again.' });
          }
          else {
            let errorStr: string = 'An unknown error occurred during registration.';
            console.error("Registration failed:\n", errorStr);
            this.messageService.add({ severity: 'error', summary: 'Registration Failed', detail: errorStr });
            this.generalErrors = [errorStr]; // template expects an array of strings to display
          }
        },
        error: (error) => {
          // Extract error message from backend response if available
          let errorStr: string = error?.error?.message || error?.message || 'An unknown error occurred during registration.';
          console.error('Registration failed:\n', errorStr);
          this.messageService.add({ severity: 'error', summary: 'Registration Failed', detail: errorStr ?? 'Please try again.' });
          this.generalErrors = [errorStr]; // template expects an array of strings to display
        }
      });
  }
}
