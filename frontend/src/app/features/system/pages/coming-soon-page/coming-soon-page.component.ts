import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-coming-soon-page',
  imports: [RouterLink],
  template: `
    <section class="page-hero">
      <div>
        <p class="page-hero__eyebrow">Placeholder route</p>
        <h1>{{ heading() }}</h1>
        <p>
          This navigation target exists so the shell can be reviewed without introducing placeholder workflows.
        </p>
      </div>
      <a class="shell-button shell-button--ghost" routerLink="/app/dashboard">Back to dashboard</a>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ComingSoonPageComponent {
  private readonly route = inject(ActivatedRoute);

  protected readonly heading = computed(() => {
    const section = this.route.snapshot.paramMap.get('section') ?? 'section';
    return `${section.charAt(0).toUpperCase()}${section.slice(1)} coming soon`;
  });
}
