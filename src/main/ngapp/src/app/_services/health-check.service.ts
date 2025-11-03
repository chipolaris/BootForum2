import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, ApplicationHealthDTO } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class HealthCheckService {

  private http = inject(HttpClient);
  private healthCheckUrl = '/api/public/health';

  constructor() { }

  /**
   * Fetches the application's health status from the backend.
   */
  checkHealth(): Observable<ApiResponse<ApplicationHealthDTO>> {
    return this.http.get<ApiResponse<ApplicationHealthDTO>>(this.healthCheckUrl);
  }
}
