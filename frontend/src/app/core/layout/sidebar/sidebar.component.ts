import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../../auth/auth.service';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { LucideIconsModule } from '../lucide-icons.module';
import { PRIMARY_NAVIGATION, SECONDARY_NAVIGATION } from '../shell-navigation.model';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, TranslatePipe, LucideIconsModule],
  template: `
    <aside class="shell-sidebar">
      <div class="shell-brand">
        <div class="shell-brand__mark">
          <lucide-icon [img]="primaryNavigation()[0]!.icon" [size]="18"></lucide-icon>
        </div>
        <div>
          <div class="shell-brand__name">{{ 'app.brand' | translate }}</div>
          <div class="shell-brand__sub">{{ 'app.workspace' | translate }}</div>
        </div>
      </div>

      <nav class="shell-nav" aria-label="Primary navigation">
        @for (item of primaryNavigation(); track item.route) {
          <a
            class="shell-nav__link"
            [routerLink]="item.route"
            routerLinkActive="is-active"
            [routerLinkActiveOptions]="{ exact: item.route === '/app/dashboard' || item.route === '/app/workspace' || item.route === '/app/notifications' }"
          >
            <lucide-icon [img]="item.icon" [size]="18"></lucide-icon>
            <span>{{ item.labelKey | translate }}</span>
            @if (item.placeholder) {
              <span class="shell-badge">{{ 'common.placeholderOnly' | translate }}</span>
            }
          </a>
        }
      </nav>

      <div class="shell-sidebar__footer">
        @for (item of secondaryNavigation(); track item.route) {
          <a class="shell-nav__link" [routerLink]="item.route">
            <lucide-icon [img]="item.icon" [size]="18"></lucide-icon>
            <span>{{ item.labelKey | translate }}</span>
          </a>
        }
      </div>
    </aside>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);

  protected readonly primaryNavigation = computed(() => this.filterNavigation(PRIMARY_NAVIGATION));
  protected readonly secondaryNavigation = computed(() => this.filterNavigation(SECONDARY_NAVIGATION));

  private filterNavigation(items: typeof PRIMARY_NAVIGATION): typeof PRIMARY_NAVIGATION {
    return items.filter((item) => this.authService.hasAnyRole(item.allowedRoles ?? []));
  }
}
