import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { LucideIconsModule } from '../../../../core/layout/lucide-icons.module';
import { NotificationEvent } from '../../../../core/notifications/notification-foundation.model';
import { NotificationFoundationService } from '../../../../core/notifications/notification-foundation.service';
import { EmptyStateComponent } from '../../../../shared/ui/empty-state/empty-state.component';
import { StatusPillComponent } from '../../../../shared/ui/status-pill/status-pill.component';

@Component({
  selector: 'app-notification-center-page',
  imports: [EmptyStateComponent, LucideIconsModule, StatusPillComponent],
  template: `
    <section class="page-hero">
      <div>
        <p class="page-hero__eyebrow">Notifications</p>
        <h1>Notification history</h1>
        <p>Outbound account email events are shown for the active organization and workspace.</p>
      </div>
      <app-status-pill [label]="summaryLabel()" tone="neutral" />
    </section>

    @if (notificationLoadError()) {
      <section class="notification-state-message notification-state-message--error" role="alert">
        {{ notificationLoadError() }}
      </section>
    }

    <section class="notification-grid">
      @for (event of notificationEvents(); track event.id) {
        <article class="notification-row">
          <div class="notification-row__icon">
            <lucide-icon name="bell-dot" [size]="18"></lucide-icon>
          </div>
          <div>
            <span class="notification-row__title">{{ notificationEventLabel(event.eventType) }}</span>
            <span class="notification-row__meta">
              {{ event.recipientDisplayName }} · {{ event.channel }} / {{ event.provider }}
            </span>
            <span class="notification-row__meta">
              {{ event.createdAt }} · {{ event.sourceId }}
            </span>
            @if (event.providerMessageId) {
              <span class="notification-row__meta">
                Provider ref {{ event.providerMessageId }} · {{ event.sentAt || event.updatedAt }}
              </span>
            }
            @if (event.failureReason) {
              <span class="notification-row__error">{{ event.failureReason }}</span>
            }
          </div>
          <app-status-pill [label]="deliveryStatusLabel(event.deliveryStatus)" [tone]="statusTone(event)" />
        </article>
      } @empty {
        <app-empty-state
          title="No notification events"
          body="Invitation and password reset notifications will appear here after account lifecycle actions."
        />
      }
    </section>
  `,
  styles: [
    `
      .notification-grid {
        display: grid;
        gap: 0.75rem;
      }

      .notification-state-message {
        margin-bottom: 1rem;
        padding: 0.75rem 0.9rem;
        border: 1px solid var(--ml-border);
        border-radius: 0.5rem;
        background: rgba(255, 255, 255, 0.035);
        color: var(--ml-text-muted);
        font-weight: 700;
      }

      .notification-state-message--error {
        border-color: rgba(214, 93, 93, 0.45);
        background: rgba(214, 93, 93, 0.12);
        color: #f2a4a4;
      }

      .notification-row {
        display: grid;
        grid-template-columns: max-content minmax(12rem, 1fr) max-content;
        gap: 0.85rem;
        align-items: center;
        min-height: 4rem;
        padding: 0.85rem 1rem;
        border: 1px solid var(--ml-border);
        border-radius: 0.5rem;
        background: rgba(255, 255, 255, 0.035);
      }

      .notification-row__icon {
        display: grid;
        place-items: center;
        width: 2.25rem;
        height: 2.25rem;
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 999px;
        color: var(--ml-accent);
      }

      .notification-row__title,
      .notification-row__meta,
      .notification-row__error {
        display: block;
      }

      .notification-row__title {
        color: var(--ml-text);
        font-size: 0.95rem;
        font-weight: 750;
      }

      .notification-row__meta {
        color: var(--ml-text-muted);
        font-size: 0.82rem;
      }

      .notification-row__error {
        color: #f2a4a4;
        font-size: 0.82rem;
      }

      @media (max-width: 720px) {
        .notification-row {
          grid-template-columns: 1fr;
          align-items: stretch;
        }

        .notification-row__icon {
          display: none;
        }
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationCenterPageComponent {
  private readonly notificationService = inject(NotificationFoundationService);
  protected readonly notificationEvents = toSignal(this.notificationService.watchNotificationEvents(), {
    initialValue: [] as NotificationEvent[],
  });
  protected readonly notificationLoadError = this.notificationService.loadError;
  protected readonly summaryLabel = computed(() => this.countLabel(this.notificationEvents().length));

  protected notificationEventLabel(eventType: string): string {
    return eventType
      .split('_')
      .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
      .join(' ');
  }

  protected statusTone(event: NotificationEvent): 'neutral' | 'ok' | 'warning' {
    if (event.deliveryStatus === 'SENT') {
      return 'ok';
    }
    if (event.deliveryStatus === 'FAILED') {
      return 'warning';
    }
    return 'neutral';
  }

  protected deliveryStatusLabel(status: string): string {
    return this.notificationEventLabel(status);
  }

  private countLabel(count: number): string {
    return `${count} event${count === 1 ? '' : 's'}`;
  }
}
