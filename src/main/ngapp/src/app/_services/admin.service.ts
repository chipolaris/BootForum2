import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private http = inject(HttpClient);
  private baseAdminApiUrl = '/api/admin';

  /**
   * Triggers a backend re-indexing process.
   * @param target The entity to re-index ('all', 'Discussion', 'Comment').
   * @returns An Observable of the API response containing a confirmation message.
   */
  triggerReindex(target: string): Observable<ApiResponse<string>> {
    // The backend expects the target as a request parameter for a POST request.
    const params = new HttpParams().set('target', target);
    // The body is null as per the backend controller's signature.
    return this.http.post<ApiResponse<string>>(`${this.baseAdminApiUrl}/indexing/reindex`, null, { params });
  }
}
