import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiResponse } from '../_data/dtos';

type VoteValue = 'up' | 'down';

@Injectable({
  providedIn: 'root'
})
export class VoteService {
  private http = inject(HttpClient);
  private baseUrl = '/api/user/vote';

  constructor() { }

  /**
   * Submits a vote for a specific discussion.
   * @param discussionId The ID of the discussion to vote on.
   * @param voteValue The vote direction, either 'up' or 'down'.
   * @returns An Observable of the API response.
   */
  voteOnDiscussion(discussionId: number, voteValue: VoteValue): Observable<ApiResponse<any>> {
    const params = new HttpParams().set('voteValue', voteValue);
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/discussion/${discussionId}`, null, { params });
  }

  /**
   * Submits a vote for a specific comment.
   * @param commentId The ID of the comment to vote on.
   * @param voteValue The vote direction, either 'up' or 'down'.
   * @returns An Observable of the API response.
   */
  voteOnComment(commentId: number, voteValue: VoteValue): Observable<ApiResponse<any>> {
    const params = new HttpParams().set('voteValue', voteValue);
    return this.http.post<ApiResponse<any>>(`${this.baseUrl}/comment/${commentId}`, null, { params });
  }
}
