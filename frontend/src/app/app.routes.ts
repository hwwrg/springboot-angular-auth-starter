import { Routes } from '@angular/router';

import { appShellGuard } from './core/guards/app-shell.guard';
import { guestShellGuard } from './core/guards/guest-shell.guard';
import { roleAccessGuard } from './core/guards/role-access.guard';
import { ShellLayoutComponent } from './core/layout/shell-layout.component';
import { AcceptInvitePageComponent } from './features/auth/pages/accept-invite-page/accept-invite-page.component';
import { ForgotPasswordPageComponent } from './features/auth/pages/forgot-password-page/forgot-password-page.component';
import { LoginShellPageComponent } from './features/auth/pages/login-shell-page/login-shell-page.component';
import { ResetPasswordPageComponent } from './features/auth/pages/reset-password-page/reset-password-page.component';
import { AccountProfilePageComponent } from './features/account/pages/account-profile-page/account-profile-page.component';
import { AdminManagementPageComponent } from './features/admin/pages/admin-management-page/admin-management-page.component';
import { DashboardShellPageComponent } from './features/dashboard/pages/dashboard-shell-page/dashboard-shell-page.component';
import { ComingSoonPageComponent } from './features/system/pages/coming-soon-page/coming-soon-page.component';
import { NotAuthorizedPageComponent } from './features/system/pages/not-authorized-page/not-authorized-page.component';
import { NotFoundPageComponent } from './features/system/pages/not-found-page/not-found-page.component';
import { NotificationCenterPageComponent } from './features/notifications/pages/notification-center-page/notification-center-page.component';
import { WorkspaceLandingPageComponent } from './features/workspace/pages/workspace-landing-page/workspace-landing-page.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'app/dashboard',
  },
  {
    path: 'login',
    canMatch: [guestShellGuard],
    component: LoginShellPageComponent,
    title: 'Spring Boot Angular Auth Starter | Login',
  },
  {
    path: 'accept-invite',
    component: AcceptInvitePageComponent,
    title: 'Spring Boot Angular Auth Starter | Accept Invitation',
  },
  {
    path: 'forgot-password',
    component: ForgotPasswordPageComponent,
    title: 'Spring Boot Angular Auth Starter | Forgot Password',
  },
  {
    path: 'reset-password',
    component: ResetPasswordPageComponent,
    title: 'Spring Boot Angular Auth Starter | Reset Password',
  },
  {
    path: 'app',
    canMatch: [appShellGuard],
    component: ShellLayoutComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'account',
        component: AccountProfilePageComponent,
        title: 'Spring Boot Angular Auth Starter | Account',
      },
      {
        path: 'dashboard',
        component: DashboardShellPageComponent,
        title: 'Spring Boot Angular Auth Starter | Dashboard',
      },
      {
        path: 'workspace',
        component: WorkspaceLandingPageComponent,
        title: 'Spring Boot Angular Auth Starter | Workspace',
      },
      {
        path: 'notifications',
        component: NotificationCenterPageComponent,
        title: 'Spring Boot Angular Auth Starter | Notifications',
      },
      {
        path: 'admin',
        canActivate: [roleAccessGuard],
        data: { roles: ['SUPERADMIN', 'ORG_ADMIN'] },
        component: AdminManagementPageComponent,
        title: 'Spring Boot Angular Auth Starter | User Management',
      },
      {
        path: 'not-authorized',
        component: NotAuthorizedPageComponent,
        title: 'Spring Boot Angular Auth Starter | Not Authorized',
      },
      {
        path: 'coming-soon/:section',
        component: ComingSoonPageComponent,
        title: 'Spring Boot Angular Auth Starter | Placeholder',
      },
    ],
  },
  {
    path: '**',
    component: NotFoundPageComponent,
    title: 'Spring Boot Angular Auth Starter | Not Found',
  },
];
