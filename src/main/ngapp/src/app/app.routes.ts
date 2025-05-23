import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { UnauthorizedComponent } from './unauthorized/unauthorized.component';
import { ResourceNotFoundComponent } from './resource-not-found/resource-not-found.component';
import { ForumTreeTableComponent } from './forum-tree-table/forum-tree-table.component';
import { ForumGroupCreateComponent } from './admin/forum-group-create/forum-group-create.component';
import { ForumGroupEditComponent } from './admin/forum-group-edit/forum-group-edit.component';
import { ForumStructureTreeComponent } from './admin/forum-structure-tree/forum-structure-tree.component';
import { ForumCreateComponent } from './admin/forum-create/forum-create.component';
import { ForumEditComponent } from './admin/forum-edit/forum-edit.component';
import { ForumListComponent } from './admin/forum-list/forum-list.component';
import { CreateDiscussionComponent } from './create-discussion/create-discussion.component';
import { RegistrationComponent } from './registration/registration.component';
import { RegistrationConfirmationComponent } from './registration-confirmation/registration-confirmation.component';
import { EmailConfirmationComponent } from './email-confirmation/email-confirmation.component';
import { LoginComponent } from './login/login.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { authGuard } from './_guards/auth.guard';

export const routes: Routes = [
  { path: 'home', component: HomeComponent },
  { path: '', redirectTo: '/home', pathMatch: 'full' }, // Redirect empty path to home
  { path: 'app/unauthorized', component: UnauthorizedComponent },
  { path: 'app/forum-tree-table', component: ForumTreeTableComponent },
  {
    path: 'app/admin/forum-structure', component: ForumStructureTreeComponent, canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'app/admin/forum-group-create', component: ForumGroupCreateComponent, canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'app/admin/forum-groups/:id', // :id is the route parameter
    component: ForumGroupEditComponent,
    canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'app/admin/forum-create', component: ForumCreateComponent, canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'app/admin/forums', component: ForumListComponent, canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'app/admin/forums/:id', component: ForumEditComponent, canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  { path: 'app/create-discussion', component: CreateDiscussionComponent },
  { path: 'app/registration', component: RegistrationComponent },
  { path: 'app/registration-confirmation', component: RegistrationConfirmationComponent },
  { path: 'app/confirm-email/:registrationKey', component: EmailConfirmationComponent },
  { path: 'app/login', component: LoginComponent },
  { path: 'app/dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'app/not-found', component: ResourceNotFoundComponent }, // Optional direct route
  { path: '**', component: ResourceNotFoundComponent }, // Wildcard route for any unmatched paths
];
