import { OrganizationContext } from '../context/organization-user-context.model';
import { NotificationEvent } from '../notifications/notification-foundation.model';

export interface AdminUserSummary {
  id: string;
  email: string;
  displayName: string;
  status: string;
  role: string;
  membershipStatus: string;
  primaryMembership: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdminUserManagementInput {
  email: string;
  displayName: string;
  userStatus: string;
  role: string;
  membershipStatus: string;
  primaryMembership: boolean;
}

export interface UpdateAdminUserManagementInput extends Omit<AdminUserManagementInput, 'email'> {
  id: string;
}

export interface AdminManagementTotals {
  userCount: number;
  notificationEventCount: number;
}

export interface AdminManagementBaseline {
  currentOrganization: OrganizationContext;
  users: AdminUserSummary[];
  notificationEvents: NotificationEvent[];
  totals: AdminManagementTotals;
}
