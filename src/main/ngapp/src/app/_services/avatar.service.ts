import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class AvatarService {
  private http = inject(HttpClient);
  private basePublicApiUrl = '/api/public';

  /**
   * Fetches a map of usernames to their corresponding avatar file IDs.
   * @param usernames An array of usernames.
   * @returns An Observable of the API response containing the map.
   */
  getAvatarFileIds(usernames: string[]): Observable<ApiResponse<Map<string, number | null>>> {
    // The backend expects a comma-separated string for the @RequestParam List<String>
    const params = new HttpParams().set('usernames', usernames.join(','));
    return this.http.get<ApiResponse<Map<string, number | null>>>(`${this.basePublicApiUrl}/avatars/ids`, { params });
  }

  /**
   * Fetches the file ID for a single user's avatar.
   * @param username The username of the user.
   * @returns An Observable of the API response containing the file ID or null.
   */
  getAvatarFileId(username: string): Observable<ApiResponse<number | null>> {
    return this.http.get<ApiResponse<number | null>>(`${this.basePublicApiUrl}/avatars/id/${username}`);
  }
}
