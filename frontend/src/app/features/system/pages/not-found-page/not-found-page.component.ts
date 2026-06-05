import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink],
  template: `
    <div class="not-found-page">
      <p class="page-hero__eyebrow">Not found</p>
      <h1>Route not found</h1>
      <p>The requested frontend shell route does not exist.</p>
      <a class="shell-button" routerLink="/app/dashboard">Go to dashboard shell</a>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundPageComponent {}

