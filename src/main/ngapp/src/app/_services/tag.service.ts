 import { Injectable, inject } from '@angular/core';
 import { HttpClient } from '@angular/common/http';
 import { Observable } from 'rxjs';
 import { ApiResponse, TagCreateDTO, TagDTO, TagUpdateDTO } from '../_data/dtos';

 @Injectable({
   providedIn: 'root'
 })
 export class TagService {
   private http = inject(HttpClient);
   private adminApiUrl = '/api/admin/tags';
   private publicApiUrl = '/api/public/tags';

   getAllTags(): Observable<ApiResponse<TagDTO[]>> {
     return this.http.get<ApiResponse<TagDTO[]>>(`${this.publicApiUrl}/all`);
   }

   createTag(payload: TagCreateDTO): Observable<ApiResponse<TagDTO>> {
     return this.http.post<ApiResponse<TagDTO>>(`${this.adminApiUrl}/create`, payload);
   }

   updateTag(id: number, payload: TagUpdateDTO): Observable<ApiResponse<TagDTO>> {
     return this.http.put<ApiResponse<TagDTO>>(`${this.adminApiUrl}/${id}`, payload);
   }

   deleteTag(id: number): Observable<ApiResponse<void>> {
     return this.http.delete<ApiResponse<void>>(`${this.adminApiUrl}/${id}`);
   }

   updateTagOrder(orderedIds: number[]): Observable<ApiResponse<void>> {
     return this.http.put<ApiResponse<void>>(`${this.adminApiUrl}/reorder`, orderedIds);
   }
 }
