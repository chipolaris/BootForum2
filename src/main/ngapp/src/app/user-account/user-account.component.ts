import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { AccountService } from '../_services/account.service';
import { AuthenticationService } from '../_services/authentication.service';
import { UserDTO } from '../_data/dtos';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-user-account',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ToastModule],
  templateUrl: './user-account.component.html',
  styleUrls: ['./user-account.component.css']
})
export class UserAccountComponent implements OnInit {
  private fb = inject(FormBuilder);
  private accountService = inject(AccountService);
  private authService = inject(AuthenticationService);
  private messageService = inject(MessageService);

  currentUser: UserDTO | null = null;
  avatarUrl: string | ArrayBuffer | null = '/assets/images/default-avatar.png';

  // Form Groups
  personInfoForm!: FormGroup;
  passwordForm!: FormGroup;

  // Loading states
  isProfileLoading = true; // Add a loading state for the initial profile fetch
  isPersonInfoLoading = false;
  isPasswordLoading = false;
  isAvatarLoading = false;

  ngOnInit(): void {
    this.initForms();
    this.loadUserProfile();
    this.loadAvatar(); // Fetch avatar on component initialization
  }

  private initForms(): void {
    this.personInfoForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]]
    });

    this.passwordForm = this.fb.group({
      oldPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]]
    });
  }

  private loadUserProfile(): void {
    this.isProfileLoading = true;
    // Use the auth service's current user if available, otherwise fetch
    const userFromAuth = this.authService.currentUserValue;
    if (userFromAuth) {
      console.log('UserAccountComponent: Populating from AuthenticationService cache.');
      this.updateComponentState(userFromAuth);
      this.isProfileLoading = false;
    } else {
      console.log('UserAccountComponent: No user in cache, fetching from API.');
      this.accountService.getMyProfile()
        .pipe(finalize(() => this.isProfileLoading = false))
        .subscribe({
          next: response => {
            if (response.success && response.data) {
              this.updateComponentState(response.data);
            } else {
              this.messageService.add({ severity: 'error', summary: 'Error', detail: 'Could not load your profile data.' });
            }
          },
          error: err => {
            console.error('UserAccountComponent: Failed to fetch user profile', err);
            this.messageService.add({ severity: 'error', summary: 'Loading Error', detail: 'An error occurred while loading your profile.' });
          }
      });
    }
  }

  /**
   * Fetches the user's avatar information and updates the avatarUrl.
   */
  private loadAvatar(): void {
    this.accountService.getMyAvatar().subscribe({
      next: (response) => {
        if (response.success && response.data?.fileInfo?.id) {
          // Construct the URL to the FileController's serveFile method
          this.avatarUrl = `/api/public/files/${response.data.fileInfo.id}`;
        } else {
          // If no avatar is found or the request fails, use the default
          this.avatarUrl = '/assets/images/default-avatar.png';
        }
      },
      error: (err) => {
        console.error("Could not fetch avatar info, using default.", err);
        this.avatarUrl = '/assets/images/default-avatar.png';
      }
    });
  }

  /**
   * Updates the component's state and populates forms with user data.
   * @param user The DTO of the currently authenticated user.
   */
  private updateComponentState(user: UserDTO): void {
    console.log('UserAccountComponent: Updating state with user object:', user);
    this.currentUser = user;

    // Safely patch the form with person info, guarding against null
    if (user && user.person) {
      this.personInfoForm.patchValue(user.person);
      console.log('UserAccountComponent: Form patched successfully with:', user.person);
    } else {
      console.warn('UserAccountComponent: User data did not contain a "person" object. Form not populated.');
    }
  }

  onUpdatePersonInfoSubmit(): void {
    if (this.personInfoForm.invalid) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Please fill out all required fields correctly.' });
      return;
    }

    this.isPersonInfoLoading = true;
    this.accountService.updatePersonInfo(this.personInfoForm.value)
      .pipe(finalize(() => this.isPersonInfoLoading = false))
      .subscribe({
        next: response => {
          if (response.success && response.data) {
            this.authService.updateCurrentUser(response.data); // Update global state
            this.updateComponentState(response.data);
            this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Your profile has been updated.' });
          } else {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: response.message || 'Failed to update profile.' });
          }
        },
        error: err => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'An unexpected error occurred.' })
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];

      // Basic validation
      if (!file.type.startsWith('image/')) {
        this.messageService.add({ severity: 'error', summary: 'Invalid File', detail: 'Please select an image file.' });
        return;
      }

      // Show preview
      const reader = new FileReader();
      reader.onload = () => this.avatarUrl = reader.result;
      reader.readAsDataURL(file);

      // Upload file
      this.isAvatarLoading = true;
      this.accountService.uploadAvatar(file)
        .pipe(finalize(() => this.isAvatarLoading = false))
        .subscribe({
          next: response => {
            if (response.success && response.data?.fileInfo.id) {
              this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Avatar updated successfully!' });
              // After a successful upload, update the avatar URL immediately
              // using the ID from the response.
              this.avatarUrl = `/api/public/files/${response.data.fileInfo.id}`;
            } else {
              this.messageService.add({ severity: 'error', summary: 'Upload Failed', detail: response.message || 'Could not upload avatar.' });
              // If upload fails, reload the last known good avatar
              this.loadAvatar();
            }
          },
          error: err => {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'An unexpected error occurred during upload.' });
            // If upload fails, reload the last known good avatar
            this.loadAvatar();
          }
        });
    }
  }

  onChangePasswordSubmit(): void {
    if (this.passwordForm.invalid) {
      this.messageService.add({ severity: 'warn', summary: 'Warning', detail: 'Please check your input for the password change.' });
      return;
    }

    this.isPasswordLoading = true;
    this.accountService.changePassword(this.passwordForm.value)
      .pipe(finalize(() => this.isPasswordLoading = false))
      .subscribe({
        next: response => {
          if (response.success) {
            this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Your password has been changed.' });
            this.passwordForm.reset();
          } else {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: response.message || 'Failed to change password.' });
          }
        },
        error: err => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'An unexpected error occurred.' })
      });
  }
}
