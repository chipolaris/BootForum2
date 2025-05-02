import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, Observable, timeout } from 'rxjs';
import { User } from '../_models/user';
import { handleHttpError } from './errorHandler';

@Injectable({
	providedIn: 'root'
})
export class UserService {

	private addUrl: string = "./api/secured/user/add";

	private editUrl: string = "./api/secured/user/edit";
	
	private profileUrl: string = "./api/secured/user/profile";

	constructor(private httpClient: HttpClient) { }

	public add(user: User): Observable<User> {

		return this.httpClient.post<User>(this.addUrl, user).pipe(
			timeout(5000), /* 5 seconds timeout */
			catchError(handleHttpError('add', this.addUrl))
		);
	}

	public edit(user: User): Observable<User> {

		return this.httpClient.put<User>(this.editUrl, user).pipe(
			timeout(5000), /* 5 seconds timeout */
			catchError(handleHttpError('edit', this.editUrl))
		);
	}
	
	public profile(): Observable<User> {
		
		return this.httpClient.get<User>(this.profileUrl).pipe(
			timeout(5000), /* 5 second timeout */
			catchError(handleHttpError('profile', this.profileUrl))
		);
	}
}
