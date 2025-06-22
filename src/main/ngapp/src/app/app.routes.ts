import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { UnauthorizedComponent } from './unauthorized/unauthorized.component';
import { ResourceNotFoundComponent } from './resource-not-found/resource-not-found.component';
import { DiscussionViewComponent } from './discussion-view/discussion-view.component';
import { ForumTreeTableComponent } from './forum-tree-table/forum-tree-table.component';
import { ForumViewComponent } from './forum-view/forum-view.component';
import { ForumGroupCreateComponent } from './admin/forum-group-create/forum-group-create.component';
import { ForumGroupEditComponent } from './admin/forum-group-edit/forum-group-edit.component';
import { ForumStructureTreeComponent } from './admin/forum-structure-tree/forum-structure-tree.component';
import { ForumCreateComponent } from './admin/forum-create/forum-create.component';
import { ForumEditComponent } from './admin/forum-edit/forum-edit.component';
import { ForumListComponent } from './admin/forum-list/forum-list.component';
import { DiscussionCreateComponent } from './discussion-create/discussion-create.component';
import { CommentCreateComponent } from './comment-create/comment-create.component';
import { RegistrationComponent } from './registration/registration.component';
import { RegistrationConfirmationComponent } from './registration-confirmation/registration-confirmation.component';
import { EmailConfirmationComponent } from './email-confirmation/email-confirmation.component';
import { LoginComponent } from './login/login.component';
import { UserProfileComponent } from './user-profile/user-profile.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { authGuard } from './_guards/auth.guard';

export const routes: Routes = [
  { path: 'home', component: HomeComponent },
  { path: '', redirectTo: '/home', pathMatch: 'full' }, // Redirect empty path to home
  { path: 'app/unauthorized', component: UnauthorizedComponent },
  { path: 'app/forums/tree-table', component: ForumTreeTableComponent },
  {
    path: 'app/admin/forum-structure', component: ForumStructureTreeComponent, canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  { path: 'app/discussions/:id/view', component: DiscussionViewComponent },
  { path: 'app/forums/:id/view', component: ForumViewComponent },
  {
    path: 'app/admin/forum-groups/create', component: ForumGroupCreateComponent, canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'app/admin/forum-groups/:id', // :id is the route parameter
    component: ForumGroupEditComponent,
    canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'app/admin/forums/create', component: ForumCreateComponent, canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'app/admin/forums/all', component: ForumListComponent, canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'app/admin/forums/:id', component: ForumEditComponent, canActivate: [authGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'app/discussions/create/:forumId',
    component: DiscussionCreateComponent, canActivate: [authGuard]
  },
  {
    path: 'app/comments/create/:discussionId', // For replying to discussion
    component: CommentCreateComponent, canActivate: [authGuard]
  },
  {
    path: 'app/comments/create/:discussionId/reply-to/:replyToId', // For replying to a specific comment
    component: CommentCreateComponent, canActivate: [authGuard]
  },
  { path: 'app/registration', component: RegistrationComponent },
  { path: 'app/registration-confirmation', component: RegistrationConfirmationComponent },
  { path: 'app/confirm-email/:registrationKey', component: EmailConfirmationComponent },
  { path: 'app/login', component: LoginComponent },
  { path: 'app/dashboard', component: DashboardComponent, canActivate: [authGuard] },
    { path: 'app/users/:username/profile', component: UserProfileComponent },
  { path: 'app/not-found', component: ResourceNotFoundComponent },
  { path: '**', component: ResourceNotFoundComponent }, // Wildcard route for any unmatched paths
];
