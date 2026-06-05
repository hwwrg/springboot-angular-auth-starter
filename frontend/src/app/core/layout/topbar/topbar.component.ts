import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../auth/auth.service';
import { I18nService } from '../../i18n/i18n.service';
import { TranslatePipe } from '../../i18n/translate.pipe';
import { LucideIconsModule } from '../lucide-icons.module';

@Component({
  selector: 'app-topbar',
  imports: [RouterLink, TranslatePipe, LucideIconsModule],
  template: `
    <header class="topbar">
      <label class="topbar-search" aria-label="Shell search">
        <lucide-icon name="search" [size]="18"></lucide-icon>
        <input type="search" [placeholder]="'topbar.searchPlaceholder' | translate" />
      </label>

      <div class="topbar-actions">
        <div class="locale-switcher" role="group" [attr.aria-label]="'topbar.language' | translate">
          @for (locale of i18nService.locales; track locale) {
            <button
              type="button"
              [class.is-active]="i18nService.locale() === locale"
              (click)="i18nService.setLocale(locale)"
            >
              {{ locale.toUpperCase() }}
            </button>
          }
        </div>

        <a class="topbar-icon-button" routerLink="/app/notifications" [attr.aria-label]="'topbar.notificationCenter' | translate">
          <lucide-icon name="bell-dot" [size]="18"></lucide-icon>
        </a>

        <div class="topbar-profile">
          <div class="topbar-profile__meta">
            <div class="topbar-profile__name">{{ 'topbar.session' | translate }}</div>
            <div class="topbar-profile__role">{{ authService.sessionLabel() }}</div>
          </div>
          <div class="topbar-profile__avatar">ML</div>
        </div>

        <button class="topbar-icon-button" type="button" (click)="logout()" [attr.aria-label]="'topbar.logout' | translate">
          <lucide-icon name="log-out" [size]="18"></lucide-icon>
        </button>
      </div>
    </header>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopbarComponent {
  protected readonly i18nService = inject(I18nService);
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'], { queryParams: { reason: 'expired' } }),
    });
  }
}
