import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  template: `
    <div class="empty-state">
      <div class="empty-state__title">{{ title() }}</div>
      <div class="empty-state__body">{{ body() }}</div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  readonly title = input.required<string>();
  readonly body = input.required<string>();
}

