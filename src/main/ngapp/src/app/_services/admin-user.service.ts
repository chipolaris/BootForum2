import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminPasswordChangeDTO, AdminUserUpdateDTO, ApiResponse, Page, UserSummaryDTO } from '../_data/dtos';
import { TableLazyLoadEvent } from 'primeng/table';

@Injectable({
  providedIn: 'root'
})
export class AdminUserService {
  private http = inject(HttpClient);
  private apiUrl = '/api/admin/users';

  getUsers(event: TableLazyLoadEvent): Observable<ApiResponse<Page<UserSummaryDTO>>> {
    let params = new HttpParams();
    const rows = event.rows ?? 10;
    params = params.set('page', (event.first ?? 0) / rows);
    params = params.set('size', rows);

    // FIX: The type of event.sortField can be string | string[] | undefined.
    // We only handle single-column sorting, so we must check if it's a string.
    if (event.sortField && typeof event.sortField === 'string') {
      // Map frontend field to backend entity path
      const sortField = this.mapSortField(event.sortField);
      params = params.set('sort', `${sortField},${event.sortOrder === 1 ? 'asc' : 'desc'}`);
    }

    return this.http.get<ApiResponse<Page<UserSummaryDTO>>>(this.apiUrl, { params });
  }

  updateUser(userId: number, dto: AdminUserUpdateDTO): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.apiUrl}/${userId}`, dto);
  }

  changePassword(userId: number, dto: AdminPasswordChangeDTO): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/${userId}/password`, dto);
  }

  private mapSortField(field: string): string {
    switch (field) {
      case 'firstName': return 'person.firstName';
      case 'lastName': return 'person.lastName';
      case 'lastLogin': return 'stat.lastLogin';
      default: return field;
    }
  }
}
