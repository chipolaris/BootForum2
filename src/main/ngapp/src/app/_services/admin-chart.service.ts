import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminChartDTO, ApiResponse } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class AdminChartService {
  private http = inject(HttpClient);
  private apiUrl = '/api/admin/charts';

  getChartData(): Observable<ApiResponse<AdminChartDTO>> {
    return this.http.get<ApiResponse<AdminChartDTO>>(this.apiUrl);
  }
}
