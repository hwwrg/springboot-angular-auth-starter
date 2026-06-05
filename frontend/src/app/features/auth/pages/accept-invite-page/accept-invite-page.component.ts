import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { OperationalErrorService } from '../../../../core/errors/operational-error.service';
import { TranslatePipe } from '../../../../core/i18n/translate.pipe';
import { LucideIconsModule } from '../../../../core/layout/lucide-icons.module';

const PASSWORD_MIN_LENGTH = 12;
const PASSWORD_MAX_LENGTH = 128;

@Component({
  selector: 'app-accept-invite-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe, LucideIconsModule],
  template: `
    <div class="auth-shell">
      <section class="auth-shell__visual">
        <div class="auth-shell__brand">{{ 'app.brand' | translate }}</div>
        <h1>{{ 'acceptInvite.title' | translate }}</h1>
        <p>{{ 'acceptInvite.subtitle' | translate }}</p>
      </section>

      <section class="auth-shell__panel">
        <div class="auth-shell__card">
          <span class="auth-shell__eyebrow">{{ 'acceptInvite.eyebrow' | translate }}</span>
          <h2>{{ 'acceptInvite.formTitle' | translate }}</h2>
          <p>{{ 'acceptInvite.supporting' | translate }}</p>

          @if (errorMessage) {
            <div class="auth-shell__error" role="alert">{{ errorMessage }}</div>
          }

          @if (successMessage) {
            <div class="auth-shell__success" role="status">
              {{ successMessage }}
              <a routerLink="/login">{{ 'acceptInvite.loginLink' | translate }}</a>
            </div>
          } @else if (hasToken) {
            <form
              class="auth-shell__form"
              [formGroup]="inviteForm"
              (ngSubmit)="submit()"
              [attr.aria-label]="'acceptInvite.formTitle' | translate"
            >
              <label>
                <span>{{ 'login.newPassword' | translate }}</span>
                <div class="password-field">
                  <input
                    [type]="passwordVisible ? 'text' : 'password'"
                    formControlName="newPassword"
                    autocomplete="new-password"
                    placeholder="password"
                  />
                  <button
                    class="password-field__toggle"
                    type="button"
                    [attr.aria-label]="(passwordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                    [attr.title]="(passwordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                    (click)="togglePasswordVisibility()"
                  >
                    <lucide-icon [name]="passwordVisible ? 'eye-off' : 'eye'" [size]="18"></lucide-icon>
                  </button>
                </div>
                @if (newPasswordError()) {
                  <small class="field-error">{{ newPasswordError() }}</small>
                }
              </label>
              <button type="submit" [disabled]="submitting">
                {{ submitting ? ('acceptInvite.submitting' | translate) : ('acceptInvite.action' | translate) }}
              </button>
            </form>
          }
        </div>
      </section>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AcceptInvitePageComponent {
  private readonly authService = inject(AuthService);
  private readonly operationalErrorService = inject(OperationalErrorService);
  private readonly route = inject(ActivatedRoute);
  private readonly token = this.route.snapshot.queryParamMap.get('token')?.trim() ?? '';

  protected readonly hasToken = this.token.length > 0;
  protected readonly inviteForm = new FormGroup({
    newPassword: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(PASSWORD_MIN_LENGTH),
        Validators.maxLength(PASSWORD_MAX_LENGTH),
      ],
    }),
  });
  protected submitting = false;
  protected passwordVisible = false;
  protected errorMessage = this.hasToken ? '' : 'Invitation token is missing. Request a new invitation link.';
  protected successMessage = '';

  protected submit(): void {
    if (!this.hasToken) {
      return;
    }
    if (this.inviteForm.invalid || this.submitting) {
      this.inviteForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    this.authService
      .acceptUserInvite({
        token: this.token,
        newPassword: this.inviteForm.controls.newPassword.value,
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.inviteForm.reset();
          this.successMessage = 'Password setup is complete. You can now sign in.';
        },
        error: (error: unknown) => {
          this.errorMessage = this.operationalErrorService.loginFailureMessage(error);
        },
      });
  }

  protected newPasswordError(): string | null {
    const control = this.inviteForm.controls.newPassword;
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

  protected togglePasswordVisibility(): void {
    this.passwordVisible = !this.passwordVisible;
  }
}
