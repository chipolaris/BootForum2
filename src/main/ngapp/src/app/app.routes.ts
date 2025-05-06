import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { CreateDiscussionComponent } from './create-discussion/create-discussion.component';
import { RegistrationComponent } from './registration/registration.component';
import { RegistrationConfirmationComponent } from './registration-confirmation/registration-confirmation.component';
import { EmailConfirmationComponent } from './email-confirmation/email-confirmation.component';
import { LoginComponent } from './login/login.component';
import { DashboardComponent } from './dashboard/dashboard.component';
/* import { ToastEditorComponent } from './toast-editor/toast-editor.component'; */
import { authGuard } from './_guards/auth.guard'; // Import the guard

export const routes: Routes = [
  { path: 'home', component: HomeComponent },
  { path: '', redirectTo: '/home', pathMatch: 'full' }, // Redirect empty path to home
      /*  { path: 'app/toast-ui-component', component: ToastEditorComponent }, */
  { path: 'app/create-discussion', component: CreateDiscussionComponent },
  { path: 'app/registration', component: RegistrationComponent },
  { path: 'app/registration-confirmation', component: RegistrationConfirmationComponent },
  { path: 'app/confirm-email/:registrationKey', component: EmailConfirmationComponent },
  { path: 'app/login', component: LoginComponent },
  { path: 'app/dashboard', component: DashboardComponent, canActivate: [authGuard] }
];
