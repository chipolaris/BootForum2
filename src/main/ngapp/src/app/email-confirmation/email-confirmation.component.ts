import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { RegistrationService } from '../_services/registration.service';
import { ApiResponse } from '../_data/dtos';
import { ProgressSpinnerModule } from 'primeng/progressspinner'; // Optional: For loading indicator

@Component({
  selector: 'app-email-confirmation',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule, // Needed for routerLink
    ProgressSpinnerModule // Optional
  ],
  templateUrl: './email-confirmation.component.html',
  styleUrls: ['./email-confirmation.component.css']
})
export class EmailConfirmationComponent implements OnInit {

  isLoading = true;
  isSuccess = false;
  isError = false;
  errorMessages: string[] = [];
  confirmationData: { [key: string]: string }  | null = null;
  generalMessage: string | null = null; // For top-level success/error message

  private route = inject(ActivatedRoute);
  private registrationService = inject(RegistrationService);
  // private router = inject(Router); // Inject if you need to navigate away

  ngOnInit(): void {
    const registrationKey = this.route.snapshot.paramMap.get('registrationKey');

    if (!registrationKey) {
      this.handleProcessingError("Registration key not found in URL.");
      return;
    }

    this.confirmRegistration(registrationKey);
  }

  private confirmRegistration(key: string): void {
    this.isLoading = true;
    this.isSuccess = false;
    this.isError = false;
    this.errorMessages = [];
    this.confirmationData = null;
    this.generalMessage = null;

    this.registrationService.confirmEmail(key)
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: (response: ApiResponse<any>) => { // Use 'any' due to differing success/error data structure
          this.generalMessage = response.message || null; // Store the general message

          if (response.success && response.data) {
            this.isSuccess = true;
            // { [key: string]: string } is equivalent to Map<String, String> in Java which is sent from backend
            this.confirmationData = response.data as { [key: string]: string };
          } else {
            // Handle logical failure indicated by the backend
            this.isError = true;
            if (response.errors && Array.isArray(response.errors)) {
              this.errorMessages = response.errors;
            } else if (response.message) {
              // Use general message as the primary error if no specific list
              this.errorMessages = [response.message];
            } else {
              this.errorMessages = ["Confirmation failed for an unknown reason."];
            }
          }
        },
        error: (err: Error) => {
          // Handle HTTP errors or errors thrown from the service's catchError
          this.handleProcessingError(err.message || "An unexpected network or server error occurred.");
        }
      });
  }

  private handleProcessingError(message: string): void {
    this.isLoading = false;
    this.isSuccess = false;
    this.isError = true;
    this.errorMessages = [message];
    this.generalMessage = "Error Processing Request"; // Set a general error title
  }
}
