import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { ApiResponse } from '../_data/dtos';

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  private http = inject(HttpClient);
  private basePublicApiUrl = '/api/public/config';

  private settingsCache = new Map<string, any>();

  getSetting(key: string): Observable<any> {
    if (this.settingsCache.has(key)) {
      return of(this.settingsCache.get(key));
    }

    return this.http.get<ApiResponse<any>>(`${this.basePublicApiUrl}/setting`, { params: { key } })
      .pipe(
        map(response => {
          if (response.success && response.data) {
            this.settingsCache.set(key, response.data);
            return response.data;
          }
          return null; // Return null on failure so the caller can use a default
        }),
        tap(data => console.log(`Fetched setting ${key}:`, data)),
        catchError(err => {
          console.error(`Error fetching setting ${key}`, err);
          return of(null); // Ensure the app doesn't break on API error
        })
      );
  }
}
