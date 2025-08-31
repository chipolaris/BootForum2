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

  /**
   * Fetches multiple settings, utilizing the cache to minimize network requests.
   * @param keys An array of setting keys to retrieve.
   * @returns An Observable that emits a map of the requested key-value pairs.
   */
  getSettings(keys: string[]): Observable<Map<string, any>> {
    const cachedSettings = new Map<string, any>();
    const keysToFetch: string[] = [];

    // 1. Segregate cached keys from keys that need to be fetched
    for (const key of keys) {
      if (this.settingsCache.has(key)) {
        cachedSettings.set(key, this.settingsCache.get(key));
      } else {
        keysToFetch.push(key);
      }
    }

    // 2. If all keys are in the cache, return them immediately
    if (keysToFetch.length === 0) {
      return of(cachedSettings);
    }

    // 3. If some keys need to be fetched, make a single API call
    return this.http.post<ApiResponse<Map<string, any>>>(`${this.basePublicApiUrl}/settings`, keysToFetch)
      .pipe(
        map(response => {
          const fetchedSettings = new Map<string, any>();
          if (response.success && response.data) {
            // Populate the map and update the cache
            for (const [key, value] of Object.entries(response.data)) {
              fetchedSettings.set(key, value);
              this.settingsCache.set(key, value);
            }
          }
          // Merge cached results with fetched results
          return new Map([...cachedSettings, ...fetchedSettings]);
        }),
        tap(data => console.log(`Batch fetched settings for keys [${keysToFetch.join(', ')}]:`, data)),
        catchError(err => {
          console.error(`Error batch fetching settings for keys [${keysToFetch.join(', ')}]`, err);
          // On error, return only the settings that were already in the cache
          return of(cachedSettings);
        })
      );
  }

  /**
   * Fetches a single setting value. If multiple setting values are needed, it is more efficient
   * to use the getSettings() method above
   */
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
