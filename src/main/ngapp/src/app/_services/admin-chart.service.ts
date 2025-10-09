import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminChartDTO, ApiResponse, ChartDataDTO } from '../_data/dtos'; // ADDED ChartDataDTO

@Injectable({
  providedIn: 'root'
})
export class AdminChartService {
  private http = inject(HttpClient);
  private apiUrl = '/api/admin/charts';

  getChartData(): Observable<ApiResponse<AdminChartDTO>> {
    return this.http.get<ApiResponse<AdminChartDTO>>(this.apiUrl);
  }

  getTopTermsChartData(): Observable<ApiResponse<ChartDataDTO>> {
    return this.http.get<ApiResponse<ChartDataDTO>>(`${this.apiUrl}/top-terms`);
  }
}
