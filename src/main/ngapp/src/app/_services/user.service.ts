import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, UserProfileDTO } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private baseUrl = '/api/public/users';

  constructor() { }

  getUserProfile(username: string): Observable<ApiResponse<UserProfileDTO>> {
    return this.http.get<ApiResponse<UserProfileDTO>>(`${this.baseUrl}/${username}/profile`);
  }
}
