import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-authorized-page',
  imports: [RouterLink],
  template: `
    <section class="page-hero">
      <div>
        <p class="page-hero__eyebrow">Role protected</p>
        <h1>Not authorized</h1>
        <p>Your current session does not include a role allowed for this workspace area.</p>
      </div>
      <a class="shell-button shell-button--ghost" routerLink="/app/dashboard">Back to dashboard</a>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotAuthorizedPageComponent {}
