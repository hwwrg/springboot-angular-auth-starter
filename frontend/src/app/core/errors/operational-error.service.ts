import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { I18nService } from '../i18n/i18n.service';

@Injectable({ providedIn: 'root' })
export class OperationalErrorService {
  private readonly i18nService = inject(I18nService);

  loginFailureMessage(error: unknown): string {
    if (this.isStatus(error, 401) || this.isStatus(error, 403)) {
      return this.i18nService.translate('login.invalidCredentials');
    }

    const graphQlMessage = this.graphQlErrorMessage(error);
    if (graphQlMessage && /^invalid email or password\.?$/i.test(graphQlMessage)) {
      return this.i18nService.translate('login.invalidCredentials');
    }
    if (graphQlMessage) {
      return graphQlMessage;
    }
    return 'Unable to sign in. Check the credentials and try again.';
  }

  adminLoadFailureMessage(error: unknown): string {
    const graphQlMessage = this.graphQlErrorMessage(error);
    if (graphQlMessage) {
      return `Admin management could not be loaded: ${graphQlMessage}`;
    }
    return 'Admin management could not be loaded. Refresh after verifying your session and role.';
  }

  adminSaveFailureMessage(error: unknown): string {
    const graphQlMessage = this.graphQlErrorMessage(error);
    if (graphQlMessage) {
      return `Save failed: ${graphQlMessage}`;
    }
    return 'Save failed. Check required fields, current-context relationships, and your role.';
  }

  notificationFailureMessage(error: unknown): string {
    const graphQlMessage = this.graphQlErrorMessage(error);
    if (graphQlMessage) {
      return `Notification status could not be loaded: ${graphQlMessage}`;
    }
    return 'Notification status could not be loaded. Refresh after verifying your session.';
  }

  private backendErrorMessage(error: unknown): string | null {
    if (!(error instanceof HttpErrorResponse)) {
      return null;
    }

    const payload = error.error as { error?: unknown } | string | null;
    if (typeof payload === 'string' && payload.trim()) {
      return payload.trim();
    }
    if (payload && typeof payload === 'object' && typeof payload.error === 'string') {
      return payload.error;
    }

    return null;
  }

  private graphQlErrorMessage(error: unknown): string | null {
    if (error instanceof Error && error.message.trim()) {
      return error.message
        .replace(/^GraphQL error:\s*/i, '')
        .replace(/^ApolloError:\s*/i, '')
        .trim();
    }

    return null;
  }

  private isStatus(error: unknown, status: number): boolean {
    return error instanceof HttpErrorResponse && error.status === status;
  }
}
