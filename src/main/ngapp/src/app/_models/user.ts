import { Person } from './person'

export interface User {
    id?: number;
    username: string;
    password?: string;
    role?: string;
    accountStatus?: string;
    person: Person;
    createDate?: Date;
    updateDate?: Date;
    createdBy?: string;
    updatedBy?: string;
}
