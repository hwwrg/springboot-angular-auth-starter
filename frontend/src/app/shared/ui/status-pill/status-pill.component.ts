import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-status-pill',
  template: `
    <span class="status-pill" [class.is-ok]="tone() === 'ok'" [class.is-warning]="tone() === 'warning'">
      {{ label() }}
    </span>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusPillComponent {
  readonly label = input.required<string>();
  readonly tone = input<'neutral' | 'ok' | 'warning'>('neutral');
}

