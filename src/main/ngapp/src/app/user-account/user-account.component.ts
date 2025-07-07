import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { AccountService } from '../_services/account.service';
import { AuthenticationService } from '../_services/authentication.service';
import { UserDTO, errorMessageFromApiResponse } from '../_data/dtos';
import { finalize } from 'rxjs';

// Custom validator function
export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const newPassword = control.get('newPassword');
  const confirmNewPassword = control.get('confirmNewPassword');

  // Don't validate if controls are not yet available or pristine
  if (!newPassword || !confirmNewPassword || !newPassword.value || !confirmNewPassword.value) {
    return null;
  }

  return newPassword.value === confirmNewPassword.value ? null : { passwordMismatch: true };
};


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
  isProfileLoading = true;
  isPersonInfoLoading = false;
  isPasswordLoading = false;
  isAvatarLoading = false;

  // --- NEW: Password visibility states ---
  oldPasswordVisible = false;
  newPasswordVisible = false;
  confirmNewPasswordVisible = false;
  // --- END NEW ---

  ngOnInit(): void {
    this.initForms();
    this.loadUserProfile();
    this.loadAvatar();
  }

  private initForms(): void {
    this.personInfoForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]]
    });

    this.passwordForm = this.fb.group({
      oldPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmNewPassword: ['', [Validators.required]]
    }, {
      validators: passwordMatchValidator
    });
  }

  private loadUserProfile(): void {
    this.isProfileLoading = true;
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

  private loadAvatar(): void {
    this.accountService.getMyAvatar().subscribe({
      next: (response) => {
        if (response.success && response.data?.fileInfo?.id) {
          this.avatarUrl = `/api/public/files/${response.data.fileInfo.id}`;
        } else {
          this.avatarUrl = '/assets/images/default-avatar.png';
        }
      },
      error: (err) => {
        console.error("Could not fetch avatar info, using default.", err);
        this.avatarUrl = '/assets/images/default-avatar.png';
      }
    });
  }

  private updateComponentState(user: UserDTO): void {
    console.log('UserAccountComponent: Updating state with user object:', user);
    this.currentUser = user;

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
            this.authService.updateCurrentUser(response.data);
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

      if (!file.type.startsWith('image/')) {
        this.messageService.add({ severity: 'error', summary: 'Invalid File', detail: 'Please select an image file.' });
        return;
      }

      const reader = new FileReader();
      reader.onload = () => this.avatarUrl = reader.result;
      reader.readAsDataURL(file);

      this.isAvatarLoading = true;
      this.accountService.uploadAvatar(file)
        .pipe(finalize(() => this.isAvatarLoading = false))
        .subscribe({
          next: response => {
            if (response.success && response.data?.fileInfo.id) {
              this.messageService.add({ severity: 'success', summary: 'Success', detail: 'Avatar updated successfully!' });
              this.avatarUrl = `/api/public/files/${response.data.fileInfo.id}`;
            } else {
              this.messageService.add({ severity: 'error', summary: 'Upload Failed', detail: response.message || 'Could not upload avatar.' });
              this.loadAvatar();
            }
          },
          error: err => {
            this.messageService.add({ severity: 'error', summary: 'Error', detail: 'An unexpected error occurred during upload.' });
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
            const errorMessage = errorMessageFromApiResponse(response);
            this.messageService.add({ severity: 'error', summary: 'Error', detail: errorMessage || 'Failed to change password.' });
          }
        },
        error: err => this.messageService.add({ severity: 'error', summary: 'Error', detail: 'An unexpected error occurred.' })
      });
  }
}
