import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, AvatarDTO, PasswordChangeDTO, PersonUpdateDTO, UserDTO } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private http = inject(HttpClient);
  private baseUrl = '/api/user'; // Base URL for authenticated user actions

  constructor() { }

  /**
   * Fetches the avatar info for the currently authenticated user.
   */
  getMyAvatar(): Observable<ApiResponse<AvatarDTO>> {
    return this.http.get<ApiResponse<AvatarDTO>>(`${this.baseUrl}/avatar/my-avatar`);
  }

  /**
   * Fetches the profile of the currently authenticated user.
   */
  getMyProfile(): Observable<ApiResponse<UserDTO>> {
    return this.http.get<ApiResponse<UserDTO>>(`${this.baseUrl}/my-profile`);
  }

  /**
   * Updates the personal information of the authenticated user.
   * @param personUpdateDTO The DTO with the new first name, last name, and email.
   */
  updatePersonInfo(personUpdateDTO: PersonUpdateDTO): Observable<ApiResponse<UserDTO>> {
    return this.http.put<ApiResponse<UserDTO>>(`${this.baseUrl}/my-profile/update-person`, personUpdateDTO);
  }

  /**
   * Uploads a new avatar for the authenticated user.
   * @param avatarFile The image file to upload.
   */
  uploadAvatar(avatarFile: File): Observable<ApiResponse<AvatarDTO>> {
    const formData = new FormData();
    formData.append('avatarFile', avatarFile, avatarFile.name);

    return this.http.post<ApiResponse<AvatarDTO>>(`${this.baseUrl}/avatar/upload`, formData);
  }

  /**
   * Changes the password for the authenticated user.
   * @param passwordChangeDTO The DTO containing the old and new passwords.
   */
  changePassword(passwordChangeDTO: PasswordChangeDTO): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/my-profile/change-password`, passwordChangeDTO);
  }
}
