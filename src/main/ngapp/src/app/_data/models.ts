export interface Person {
	id?: number;
	firstName?: string;
	lastName?: string;
	email?: string;
	createDate?: Date;
	updateDate?: Date;
	createdBy?: string;
	updatedBy?: string;
}

export interface User {
  id?: number;
  username: string;
  password?: string;
  userRoles?: string[];
  accountStatus?: string;
  person: Person;
  createDate?: Date;
  updateDate?: Date;
  createdBy?: string;
  updatedBy?: string;
}
