import { Injectable, inject, signal } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { catchError, map, merge, of, Subject, switchMap, tap } from 'rxjs';

import { OperationalErrorService } from '../errors/operational-error.service';
import { NotificationEvent } from './notification-foundation.model';

interface NotificationEventsResponse {
  notificationEvents: NotificationEvent[];
}

const NOTIFICATION_EVENT_FIELDS = gql`
  fragment NotificationEventFields on NotificationEvent {
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

const NOTIFICATION_EVENTS_QUERY = gql`
  ${NOTIFICATION_EVENT_FIELDS}
  query NotificationEvents {
    notificationEvents {
      ...NotificationEventFields
    }
  }
`;

@Injectable({ providedIn: 'root' })
export class NotificationFoundationService {
  private readonly apollo = inject(Apollo);
  private readonly operationalErrorService = inject(OperationalErrorService);
  private readonly refreshEvents = new Subject<void>();
  private readonly loadErrorState = signal<string | null>(null);

  readonly loadError = this.loadErrorState.asReadonly();

  watchNotificationEvents() {
    return merge(of(undefined), this.refreshEvents).pipe(
      switchMap(() =>
        this.apollo.query<NotificationEventsResponse>({
          query: NOTIFICATION_EVENTS_QUERY,
          fetchPolicy: 'network-only',
        }),
      ),
      tap(() => this.loadErrorState.set(null)),
      map(({ data }) => (data?.notificationEvents ?? []) as NotificationEvent[]),
      catchError((error: unknown) => {
        console.warn('Notification event query failed.', error);
        this.loadErrorState.set(this.operationalErrorService.notificationFailureMessage(error));
        return of([]);
      }),
    );
  }

  refreshNotificationEvents(): void {
    this.refreshEvents.next();
  }
}
