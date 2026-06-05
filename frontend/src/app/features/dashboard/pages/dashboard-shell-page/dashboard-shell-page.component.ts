import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { ReadinessService } from '../../../../core/graphql/readiness.service';
import { TranslatePipe } from '../../../../core/i18n/translate.pipe';
import { LucideIconsModule } from '../../../../core/layout/lucide-icons.module';
import { PlaceholderCardComponent } from '../../../../shared/ui/placeholder-card/placeholder-card.component';
import { StatusPillComponent } from '../../../../shared/ui/status-pill/status-pill.component';

@Component({
  selector: 'app-dashboard-shell-page',
  imports: [TranslatePipe, PlaceholderCardComponent, StatusPillComponent, LucideIconsModule],
  template: `
    <section class="page-hero">
      <div>
        <p class="page-hero__eyebrow">{{ 'shell.placeholder' | translate }}</p>
        <h1>{{ 'dashboard.title' | translate }}</h1>
        <p>{{ 'dashboard.subtitle' | translate }}</p>
      </div>
      <app-status-pill [label]="readinessLabel()" [tone]="readinessTone()" />
    </section>

    <section class="page-grid">
      <app-placeholder-card
        eyebrow="GraphQL"
        [title]="'dashboard.backendStatus' | translate"
        [description]="'dashboard.connectivity' | translate"
      >
        <div class="card-stat">
          <lucide-icon name="server" [size]="18"></lucide-icon>
          <div>
            <strong>{{ readinessApplication() }}</strong>
            <p>{{ readinessDescription() }}</p>
          </div>
        </div>
      </app-placeholder-card>

      <app-placeholder-card
        eyebrow="UI"
        [title]="'dashboard.shellProgress' | translate"
        [description]="'dashboard.uiReference' | translate"
      >
        <ul class="shell-checklist">
          <li>Login, logout, CSRF, session bootstrap, and route guards are wired.</li>
          <li>User invitations, first password setup, password reset, and password change are available.</li>
          <li>Admin user management is scoped to the active organization context and RBAC role.</li>
        </ul>
      </app-placeholder-card>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardShellPageComponent {
  private readonly readiness = toSignal(inject(ReadinessService).watchReadiness(), {
    initialValue: undefined,
  });

  protected readonly readinessTone = computed<'neutral' | 'ok' | 'warning'>(() => {
    const state = this.readiness();
    if (state === undefined) {
      return 'neutral';
    }

    return state ? 'ok' : 'warning';
  });

  protected readonly readinessLabel = computed(() => {
    const state = this.readiness();
    if (state === undefined) {
      return 'Checking backend';
    }

    return state ? `Backend ${state.status}` : 'Backend unavailable';
  });

  protected readonly readinessApplication = computed(() => this.readiness()?.application ?? 'backend /graphql');

  protected readonly readinessDescription = computed(() => {
    const state = this.readiness();
    if (state === undefined) {
      return 'Waiting for the GraphQL readiness query response.';
    }

    return state
      ? 'The frontend shell is connected to the Spring Boot readiness query.'
      : 'The frontend shell is ready, but the backend was not reachable during this check.';
  });
}
