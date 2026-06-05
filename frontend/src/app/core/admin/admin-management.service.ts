import { Injectable, inject, signal } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { catchError, finalize, map, of } from 'rxjs';

import { OperationalErrorService } from '../errors/operational-error.service';
import {
  AdminManagementBaseline,
  AdminUserManagementInput,
  AdminUserSummary,
  UpdateAdminUserManagementInput,
} from './admin-management.model';

interface AdminManagementBaselineResponse {
  adminManagementBaseline: AdminManagementBaseline;
}

interface AdminCreateUserResponse {
  adminCreateUser: AdminUserSummary;
}

interface AdminUpdateUserResponse {
  adminUpdateUser: AdminUserSummary;
}

const ADMIN_USER_FIELDS = gql`
  fragment AdminUserFields on AdminUserSummary {
    id
    email
    displayName
    status
    role
    membershipStatus
    primaryMembership
    createdAt
    updatedAt
  }
`;

const NOTIFICATION_EVENT_FIELDS = gql`
  fragment AdminNotificationEventFields on NotificationEvent {
    id
    organizationId
    workspaceId
    sourceType
    sourceId
    eventType
    recipientType
    recipientId
    recipientDisplayName
    recipientEmail
    channel
    provider
    deliveryStatus
    providerMessageId
    failureReason
    createdAt
    sentAt
    updatedAt
  }
`;

const ADMIN_MANAGEMENT_BASELINE_QUERY = gql`
  query AdminManagementBaseline {
    adminManagementBaseline {
      currentOrganization {
        organizationId
        organizationDisplayName
        organizationStatus
        workspaceId
        workspaceCode
        workspaceStatus
        role
      }
      users {
        ...AdminUserFields
      }
      notificationEvents {
        ...AdminNotificationEventFields
      }
      totals {
        userCount
        notificationEventCount
      }
    }
  }
  ${ADMIN_USER_FIELDS}
  ${NOTIFICATION_EVENT_FIELDS}
`;

const ADMIN_CREATE_USER_MUTATION = gql`
  mutation AdminCreateUser($input: CreateAdminUserInput!) {
    adminCreateUser(input: $input) {
      ...AdminUserFields
    }
  }
  ${ADMIN_USER_FIELDS}
`;

const ADMIN_UPDATE_USER_MUTATION = gql`
  mutation AdminUpdateUser($input: UpdateAdminUserInput!) {
    adminUpdateUser(input: $input) {
      ...AdminUserFields
    }
  }
  ${ADMIN_USER_FIELDS}
`;

@Injectable({ providedIn: 'root' })
export class AdminManagementService {
  private readonly apollo = inject(Apollo);
  private readonly operationalErrorService = inject(OperationalErrorService);
  private readonly savingState = signal(false);
  private readonly saveErrorState = signal<string | null>(null);

  readonly saving = this.savingState.asReadonly();
  readonly saveError = this.saveErrorState.asReadonly();

  fetchBaseline() {
    return this.apollo
      .query<AdminManagementBaselineResponse>({
        query: ADMIN_MANAGEMENT_BASELINE_QUERY,
        fetchPolicy: 'network-only',
      })
      .pipe(
        map(({ data }) => data?.adminManagementBaseline ?? null),
        catchError((error: unknown) => {
          console.warn('Admin baseline query failed.', error);
          return of(null);
        }),
      );
  }

  createUser(input: AdminUserManagementInput) {
    this.savingState.set(true);
    this.saveErrorState.set(null);
    return this.apollo
      .mutate<AdminCreateUserResponse>({
        mutation: ADMIN_CREATE_USER_MUTATION,
        variables: { input },
      })
      .pipe(
        map(({ data }) => data?.adminCreateUser ?? null),
        catchError((error: unknown) => {
          this.saveErrorState.set(this.operationalErrorService.adminSaveFailureMessage(error));
          return of(null);
        }),
        finalize(() => this.savingState.set(false)),
      );
  }

  updateUser(input: UpdateAdminUserManagementInput) {
    this.savingState.set(true);
    this.saveErrorState.set(null);
    return this.apollo
      .mutate<AdminUpdateUserResponse>({
        mutation: ADMIN_UPDATE_USER_MUTATION,
        variables: { input },
      })
      .pipe(
        map(({ data }) => data?.adminUpdateUser ?? null),
        catchError((error: unknown) => {
          this.saveErrorState.set(this.operationalErrorService.adminSaveFailureMessage(error));
          return of(null);
        }),
        finalize(() => this.savingState.set(false)),
      );
  }
}
