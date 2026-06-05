import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-placeholder-card',
  template: `
    <section class="placeholder-card">
      <div class="placeholder-card__eyebrow">{{ eyebrow() }}</div>
      <h2>{{ title() }}</h2>
      <p>{{ description() }}</p>
      <ng-content></ng-content>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlaceholderCardComponent {
  readonly eyebrow = input.required<string>();
  readonly title = input.required<string>();
  readonly description = input.required<string>();
}

