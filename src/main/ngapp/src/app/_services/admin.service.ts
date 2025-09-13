import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse, DiscussionSimulationConfigDTO, SettingDTO } from '../_data/dtos';

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

  /**
   * Triggers backend generation of simulated users.
   * @param count The number of users to generate.
   */
  triggerUserSimulation(count: number): Observable<ApiResponse<string>> {
    const params = new HttpParams().set('count', count.toString());
    return this.http.post<ApiResponse<string>>(`${this.baseAdminApiUrl}/data/simulate-users`, null, { params });
  }

  /**
   * Triggers backend generation of simulated forum data (groups, forums, discussions, comments).
   * @param config The configuration for the simulation.
   */
  triggerDiscussionSimulation(config: DiscussionSimulationConfigDTO): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.baseAdminApiUrl}/data/simulate-discussions`, config);
  }

  /**
   * Triggers backend generation of simulated votes for discussions and comments.
   */
  triggerVoteSimulation(): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.baseAdminApiUrl}/data/simulate-votes`, null);
  }

  /**
   * Fetches all forum settings, grouped by category.
   */
  getForumSettings(): Observable<ApiResponse<Map<string, SettingDTO[]>>> {
    return this.http.get<ApiResponse<Map<string, SettingDTO[]>>>(`${this.baseAdminApiUrl}/forum-settings/get`);
  }

  /**
   * Updates multiple forum settings.
   * @param settings The settings map to be saved.
   */
  updateForumSettings(settings: Map<string, SettingDTO[]>): Observable<ApiResponse<void>> {
    // Convert the Map to a plain object for the HTTP request body, as JSON standard doesn't have a Map type.
    const payload = Object.fromEntries(settings);
    return this.http.post<ApiResponse<void>>(`${this.baseAdminApiUrl}/forum-settings/update`, payload);
  }
}
