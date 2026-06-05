import { ChangeDetectionStrategy, Component } from '@angular/core';

import { TranslatePipe } from '../../../../core/i18n/translate.pipe';
import { EmptyStateComponent } from '../../../../shared/ui/empty-state/empty-state.component';
import { PlaceholderCardComponent } from '../../../../shared/ui/placeholder-card/placeholder-card.component';

@Component({
  selector: 'app-workspace-landing-page',
  imports: [TranslatePipe, PlaceholderCardComponent, EmptyStateComponent],
  template: `
    <section class="page-hero">
      <div>
        <p class="page-hero__eyebrow">{{ 'shell.reference' | translate }}</p>
        <h1>Starter workspace</h1>
        <p>Reusable account, RBAC, invitation, password reset, and notification foundations.</p>
      </div>
    </section>

    <section class="page-grid">
      <app-placeholder-card
        eyebrow="Reference"
        title="Reusable auth surface"
        description="This workspace intentionally contains only generic authentication and user-management modules."
      ></app-placeholder-card>

      <app-empty-state
        title="Extension point"
        body="Add your own domain modules here after wiring them to the retained authentication and RBAC foundation."
      />
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceLandingPageComponent {}
