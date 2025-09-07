import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, AdminDashboardDTO } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class AdminDashboardService {
  private http = inject(HttpClient);
  private apiUrl = '/api/admin/dashboard';

  getDashboardData(timeWindow: string): Observable<ApiResponse<AdminDashboardDTO>> {
    const params = new HttpParams().set('timeWindow', timeWindow);
    return this.http.get<ApiResponse<AdminDashboardDTO>>(this.apiUrl, { params });
  }
}
