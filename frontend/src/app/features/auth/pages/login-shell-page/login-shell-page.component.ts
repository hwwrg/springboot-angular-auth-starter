import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { OperationalErrorService } from '../../../../core/errors/operational-error.service';
import { TranslatePipe } from '../../../../core/i18n/translate.pipe';
import { LucideIconsModule } from '../../../../core/layout/lucide-icons.module';
import { RuntimeConfigService } from '../../../../core/runtime-config/runtime-config.service';

const PASSWORD_MIN_LENGTH = 12;
const PASSWORD_MAX_LENGTH = 128;

@Component({
  selector: 'app-login-shell-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe, LucideIconsModule],
  template: `
    <div class="auth-shell">
      <section class="auth-shell__visual">
        <div class="auth-shell__brand">{{ 'app.brand' | translate }}</div>
        <h1>{{ 'login.title' | translate }}</h1>
        <p>{{ 'login.subtitle' | translate }}</p>
        <p class="auth-shell__version">{{ 'login.version' | translate }} {{ appVersion() }}</p>
      </section>

      <section class="auth-shell__panel">
        <div class="auth-shell__card">
          <span class="auth-shell__eyebrow">{{ 'login.eyebrow' | translate }}</span>
          <h2>{{ 'login.title' | translate }}</h2>
          <p>{{ recoveryPasswordRequired ? ('login.recoverySupporting' | translate) : ('login.supporting' | translate) }}</p>

          @if (errorMessage) {
            <div class="auth-shell__error" role="alert">{{ errorMessage }}</div>
          }

          @if (recoveryPasswordRequired) {
            <form
              class="auth-shell__form"
              [formGroup]="passwordChangeForm"
              (ngSubmit)="submitPasswordChange()"
              [attr.aria-label]="'login.recoveryTitle' | translate"
            >
              <label>
                <span>{{ 'login.newPassword' | translate }}</span>
                <div class="password-field">
                  <input
                    [type]="recoveryPasswordVisible ? 'text' : 'password'"
                    formControlName="newPassword"
                    autocomplete="new-password"
                    placeholder="password"
                  />
                  <button
                    class="password-field__toggle"
                    type="button"
                    [attr.aria-label]="(recoveryPasswordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                    [attr.title]="(recoveryPasswordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                    (click)="toggleRecoveryPasswordVisibility()"
                  >
                    <lucide-icon [name]="recoveryPasswordVisible ? 'eye-off' : 'eye'" [size]="18"></lucide-icon>
                  </button>
                </div>
                @if (newPasswordError()) {
                  <small class="field-error">{{ newPasswordError() }}</small>
                }
              </label>
              <button type="submit" [disabled]="submitting">
                {{ submitting ? ('login.passwordChangeSubmitting' | translate) : ('login.passwordChangeAction' | translate) }}
              </button>
            </form>
          } @else {
            <form class="auth-shell__form" [formGroup]="loginForm" (ngSubmit)="submit()" [attr.aria-label]="'login.title' | translate">
              <label>
                <span>{{ 'login.email' | translate }}</span>
                <input type="email" formControlName="email" autocomplete="username" placeholder="name@example.com" />
                @if (emailError()) {
                  <small class="field-error">{{ emailError() }}</small>
                }
              </label>
              <label>
                <span>{{ 'login.password' | translate }}</span>
                <div class="password-field">
                  <input
                    [type]="loginPasswordVisible ? 'text' : 'password'"
                    formControlName="password"
                    autocomplete="current-password"
                    placeholder="password"
                  />
                  <button
                    class="password-field__toggle"
                    type="button"
                    [attr.aria-label]="(loginPasswordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                    [attr.title]="(loginPasswordVisible ? 'common.hidePassword' : 'common.showPassword') | translate"
                    (click)="toggleLoginPasswordVisibility()"
                  >
                    <lucide-icon [name]="loginPasswordVisible ? 'eye-off' : 'eye'" [size]="18"></lucide-icon>
                  </button>
                </div>
                @if (passwordError()) {
                  <small class="field-error">{{ passwordError() }}</small>
                }
              </label>
              <button type="submit" [disabled]="submitting">
                {{ submitting ? ('login.submitting' | translate) : ('login.action' | translate) }}
              </button>
              <a class="auth-shell__secondary-link" routerLink="/forgot-password">
                {{ 'login.forgotPassword' | translate }}
              </a>
            </form>
          }
        </div>
      </section>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginShellPageComponent {
  private readonly authService = inject(AuthService);
  private readonly operationalErrorService = inject(OperationalErrorService);
  private readonly runtimeConfigService = inject(RuntimeConfigService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly appVersion = () => this.runtimeConfigService.config().appVersion;

  protected readonly loginForm = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });
  protected readonly passwordChangeForm = new FormGroup({
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
  protected recoveryPasswordRequired = this.authService.mustChangePassword();
  protected loginPasswordVisible = false;
  protected recoveryPasswordVisible = false;
  protected errorMessage =
    this.route.snapshot.queryParamMap.get('reason') === 'expired'
      ? 'Your session expired. Sign in again to continue.'
      : '';

  protected submit(): void {
    if (this.loginForm.invalid || this.submitting) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    this.authService
      .login(this.loginForm.getRawValue())
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          if (this.authService.mustChangePassword()) {
            this.recoveryPasswordRequired = true;
            return;
          }
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/app/dashboard';
          this.router.navigateByUrl(returnUrl);
        },
        error: (error: unknown) => {
          this.errorMessage = this.operationalErrorService.loginFailureMessage(error);
        },
      });
  }

  protected submitPasswordChange(): void {
    if (this.passwordChangeForm.invalid || this.submitting) {
      this.passwordChangeForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = '';

    this.authService
      .changeOwnPassword(this.passwordChangeForm.getRawValue())
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.recoveryPasswordRequired = false;
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/app/dashboard';
          this.router.navigateByUrl(returnUrl);
        },
        error: (error: unknown) => {
          this.errorMessage = this.operationalErrorService.loginFailureMessage(error);
        },
      });
  }

  protected toggleLoginPasswordVisibility(): void {
    this.loginPasswordVisible = !this.loginPasswordVisible;
  }

  protected toggleRecoveryPasswordVisibility(): void {
    this.recoveryPasswordVisible = !this.recoveryPasswordVisible;
  }

  protected emailError(): string | null {
    const control = this.loginForm.controls.email;
    if (!control.touched && !control.dirty) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Email is required.';
    }
    if (control.hasError('email')) {
      return 'Enter a valid email address.';
    }
    return null;
  }

  protected passwordError(): string | null {
    const control = this.loginForm.controls.password;
    if (!control.touched && !control.dirty) {
      return null;
    }
    return control.hasError('required') ? 'Password is required.' : null;
  }

  protected newPasswordError(): string | null {
    const control = this.passwordChangeForm.controls.newPassword;
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
}
