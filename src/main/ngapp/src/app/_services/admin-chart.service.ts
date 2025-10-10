import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminChartDTO, ApiResponse, ChartDataDTO } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class AdminChartService {
  private http = inject(HttpClient);
  private apiUrl = '/api/admin/charts';

  getChartData(): Observable<ApiResponse<AdminChartDTO>> {
    return this.http.get<ApiResponse<AdminChartDTO>>(this.apiUrl);
  }

  getTopTermsChartData(limit: number, period: string): Observable<ApiResponse<ChartDataDTO>> {
    const params = new HttpParams()
      .set('limit', limit.toString())
      .set('period', period);
    return this.http.get<ApiResponse<ChartDataDTO>>(`${this.apiUrl}/top-terms`, { params });
  }
}
