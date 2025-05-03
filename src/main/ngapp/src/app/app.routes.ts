import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { RegistrationComponent } from './registration/registration.component';
import { LoginComponent } from './login/login.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { authGuard } from './_guards/auth.guard'; // Import the guard

export const routes: Routes = [
  { path: 'home', component: HomeComponent },
  { path: '', redirectTo: '/home', pathMatch: 'full' }, // Redirect empty path to home
  { path: 'app/register', component: RegistrationComponent },
  { path: 'app/login', component: LoginComponent },
  { path: 'app/dashboard', component: DashboardComponent, canActivate: [authGuard] }
];
