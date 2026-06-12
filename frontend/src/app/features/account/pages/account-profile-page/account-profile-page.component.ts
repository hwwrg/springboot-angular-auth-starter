import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { OperationalErrorService } from '../../../../core/errors/operational-error.service';
import { TranslatePipe } from '../../../../core/i18n/translate.pipe';
import { LucideIconsModule } from '../../../../core/layout/lucide-icons.module';
import { StatusPillComponent } from '../../../../shared/ui/status-pill/status-pill.component';
import { MfaManagementComponent } from '../../components/mfa-management/mfa-management.component';

const PASSWORD_MIN_LENGTH = 12;
const PASSWORD_MAX_LENGTH = 128;

@Component({
  selector: 'app-account-profile-page',
  imports: [ReactiveFormsModule, TranslatePipe, LucideIconsModule, StatusPillComponent, MfaManagementComponent],
  template: `
    <section class="page-hero">
      <div>
        <p class="page-hero__eyebrow">{{ 'account.eyebrow' | translate }}</p>
        <h1>{{ 'account.title' | translate }}</h1>
        <p>{{ 'account.subtitle' | translate }}</p>
      </div>
    </section>

    <section class="context-grid">
      <article class="context-panel">
        <div class="context-panel__header">
          <div>
            <p class="context-panel__eyebrow">{{ 'account.userContext' | translate }}</p>
            <h2>{{ userProfile()?.displayName ?? authService.session().principal?.displayName }}</h2>
          </div>
          <app-status-pill [label]="userProfile()?.status ?? 'SESSION'" tone="ok" />
        </div>

        <dl class="context-list">
          <div>
            <dt>{{ 'account.email' | translate }}</dt>
            <dd>{{ userProfile()?.email ?? authService.session().principal?.email }}</dd>
          </div>
          <div>
            <dt>{{ 'account.roles' | translate }}</dt>
            <dd>{{ authService.roles().join(', ') }}</dd>
          </div>
        </dl>
      </article>

      <article class="context-panel">
        <div class="context-panel__header">
          <div>
            <p class="context-panel__eyebrow">{{ 'account.organizationContext' | translate }}</p>
            <h2>{{ currentOrganization()?.organizationDisplayName ?? ('account.noOrganizationContext' | translate) }}</h2>
          </div>
          @if (currentOrganization()) {
            <app-status-pill [label]="currentOrganization()!.organizationStatus" tone="ok" />
          }
        </div>

        @if (currentOrganization(); as organization) {
          <dl class="context-list">
            <div>
              <dt>{{ 'account.workspace' | translate }}</dt>
              <dd>{{ organization.workspaceCode }}</dd>
            </div>
            <div>
              <dt>{{ 'account.organizationRole' | translate }}</dt>
              <dd>{{ organization.role }}</dd>
            </div>
          </dl>
        } @else {
          <p class="context-panel__empty">{{ 'account.noOrganizationContextBody' | translate }}</p>
        }
      </article>

      <article class="context-panel">
        <div class="context-panel__header">
          <div>
            <p class="context-panel__eyebrow">{{ 'account.passwordTitle' | translate }}</p>
            <h2>{{ 'account.passwordTitle' | translate }}</h2>
          </div>
        </div>
        <p class="context-panel__empty">{{ 'account.passwordSupporting' | translate }}</p>

        @if (passwordMessage) {
          <div
            class="auth-shell__success account-password-message"
            [class.auth-shell__success--error]="passwordMessageTone === 'error'"
            [attr.role]="passwordMessageTone === 'error' ? 'alert' : 'status'"
          >
            {{ passwordMessage }}
          </div>
        }

        <form
          class="auth-shell__form account-password-form"
          [formGroup]="passwordForm"
          (ngSubmit)="submitPasswordChange()"
        >
          <label>
            <span>{{ 'login.password' | translate }}</span>
            <div class="password-field">
              <input
                [type]="currentPasswordVisible ? 'text' : 'password'"
                formControlName="currentPassword"
                autocomplete="current-password"
                placeholder="password"
              />
              <button
                class="password-field__toggle"
                type="button"
                [attr.aria-label]="(currentPasswordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                [attr.title]="(currentPasswordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                (click)="toggleCurrentPasswordVisibility()"
              >
                <lucide-icon [name]="currentPasswordVisible ? 'eye-off' : 'eye'" [size]="18"></lucide-icon>
              </button>
            </div>
            @if (currentPasswordError()) {
              <small class="field-error">{{ currentPasswordError() }}</small>
            }
          </label>
          <label>
            <span>{{ 'login.newPassword' | translate }}</span>
            <div class="password-field">
              <input
                [type]="newPasswordVisible ? 'text' : 'password'"
                formControlName="newPassword"
                autocomplete="new-password"
                placeholder="password"
              />
              <button
                class="password-field__toggle"
                type="button"
                [attr.aria-label]="(newPasswordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                [attr.title]="(newPasswordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                (click)="toggleNewPasswordVisibility()"
              >
                <lucide-icon [name]="newPasswordVisible ? 'eye-off' : 'eye'" [size]="18"></lucide-icon>
              </button>
            </div>
            @if (newPasswordError()) {
              <small class="field-error">{{ newPasswordError() }}</small>
            }
          </label>
          <label>
            <span>{{ 'account.confirmPassword' | translate }}</span>
            <div class="password-field">
              <input
                [type]="confirmPasswordVisible ? 'text' : 'password'"
                formControlName="confirmPassword"
                autocomplete="new-password"
                placeholder="password"
              />
              <button
                class="password-field__toggle"
                type="button"
                [attr.aria-label]="(confirmPasswordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                [attr.title]="(confirmPasswordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                (click)="toggleConfirmPasswordVisibility()"
              >
                <lucide-icon [name]="confirmPasswordVisible ? 'eye-off' : 'eye'" [size]="18"></lucide-icon>
              </button>
            </div>
            @if (confirmPasswordError()) {
              <small class="field-error">{{ confirmPasswordError() }}</small>
            }
          </label>
          <button type="submit" [disabled]="submittingPassword">
            {{ submittingPassword ? ('account.passwordSubmitting' | translate) : ('account.passwordAction' | translate) }}
          </button>
        </form>
      </article>

      <article class="context-panel context-panel--wide">
        <div class="context-panel__header">
          <div>
            <p class="context-panel__eyebrow">{{ 'account.memberships' | translate }}</p>
            <h2>{{ 'account.membershipScope' | translate }}</h2>
          </div>
        </div>

        <div class="membership-table" role="table">
          @for (membership of userProfile()?.memberships ?? []; track membership.organizationId) {
            <div class="membership-table__row" role="row">
              <span>{{ membership.organizationDisplayName }}</span>
              <span>{{ membership.workspaceCode }}</span>
              <span>{{ membership.role }}</span>
              <app-status-pill [label]="membership.status" tone="ok" />
            </div>
          } @empty {
            <p class="context-panel__empty">{{ 'account.noMemberships' | translate }}</p>
          }
        </div>
      </article>

      <app-mfa-management />
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountProfilePageComponent {
  protected readonly authService = inject(AuthService);
  private readonly operationalErrorService = inject(OperationalErrorService);
  protected readonly userProfile = this.authService.userProfile;
  protected readonly currentOrganization = this.authService.currentOrganization;
  protected readonly passwordForm = new FormGroup({
    currentPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    newPassword: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(PASSWORD_MIN_LENGTH),
        Validators.maxLength(PASSWORD_MAX_LENGTH),
      ],
    }),
    confirmPassword: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });
  protected submittingPassword = false;
  protected passwordMessage = '';
  protected passwordMessageTone: 'success' | 'error' = 'success';
  protected currentPasswordVisible = false;
  protected newPasswordVisible = false;
  protected confirmPasswordVisible = false;

  protected submitPasswordChange(): void {
    if (
      this.passwordForm.invalid ||
      this.passwordForm.controls.newPassword.value !== this.passwordForm.controls.confirmPassword.value ||
      this.submittingPassword
    ) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.submittingPassword = true;
    this.passwordMessage = '';
    this.passwordMessageTone = 'success';

    this.authService
      .changeOwnPassword({
        currentPassword: this.passwordForm.controls.currentPassword.value,
        newPassword: this.passwordForm.controls.newPassword.value,
      })
      .pipe(finalize(() => (this.submittingPassword = false)))
      .subscribe({
        next: () => {
          this.passwordForm.reset();
          this.passwordMessageTone = 'success';
          this.passwordMessage = 'Password changed.';
        },
        error: (error: unknown) => {
          this.passwordMessageTone = 'error';
          this.passwordMessage = this.operationalErrorService.loginFailureMessage(error);
        },
      });
  }

  protected newPasswordError(): string | null {
    const control = this.passwordForm.controls.newPassword;
    if (!control.touched && !control.dirty) {
      return null;
    }
    if (control.hasError('required')) {
      return 'New password is required.';
    }
    if (control.hasError('minlength') || control.hasError('maxlength')) {
      return `Password must be ${PASSWORD_MIN_LENGTH} to ${PASSWORD_MAX_LENGTH} characters.`;
    }
    return null;
  }

  protected toggleCurrentPasswordVisibility(): void {
    this.currentPasswordVisible = !this.currentPasswordVisible;
  }

  protected toggleNewPasswordVisibility(): void {
    this.newPasswordVisible = !this.newPasswordVisible;
  }

  protected toggleConfirmPasswordVisibility(): void {
    this.confirmPasswordVisible = !this.confirmPasswordVisible;
  }

  protected currentPasswordError(): string | null {
    const control = this.passwordForm.controls.currentPassword;
    if (!control.touched && !control.dirty) {
      return null;
    }
    return control.hasError('required') ? 'Current password is required.' : null;
  }

  protected confirmPasswordError(): string | null {
    const control = this.passwordForm.controls.confirmPassword;
    if (!control.touched && !control.dirty) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Confirm password is required.';
    }
    return this.passwordForm.controls.newPassword.value !== control.value ? 'Passwords do not match.' : null;
  }
}
