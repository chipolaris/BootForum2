import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, UserReputationDTO } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class UserReputationService {
  private http = inject(HttpClient);
  private baseUrl = '/api/user'; // Authenticated user endpoints

  constructor() { }

  getMyReputation(): Observable<ApiResponse<UserReputationDTO>> {
    return this.http.get<ApiResponse<UserReputationDTO>>(`${this.baseUrl}/my-reputation`);
  }
}
