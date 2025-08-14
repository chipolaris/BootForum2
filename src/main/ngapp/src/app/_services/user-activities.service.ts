import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, MyActivitiesDTO } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class UserActivitiesService {
  private http = inject(HttpClient);
  private baseUrl = '/api/user'; // Authenticated user endpoints

  constructor() { }

  getMyActivities(): Observable<ApiResponse<MyActivitiesDTO>> {
    return this.http.get<ApiResponse<MyActivitiesDTO>>(`${this.baseUrl}/my-activities`);
  }
}
