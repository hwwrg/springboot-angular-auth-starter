import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { TranslatePipe } from '../../../../core/i18n/translate.pipe';

@Component({
  selector: 'app-forgot-password-page',
  imports: [ReactiveFormsModule, RouterLink, TranslatePipe],
  template: `
    <div class="auth-shell">
      <section class="auth-shell__visual">
        <div class="auth-shell__brand">{{ 'app.brand' | translate }}</div>
        <h1>{{ 'forgotPassword.title' | translate }}</h1>
        <p>{{ 'forgotPassword.subtitle' | translate }}</p>
      </section>

      <section class="auth-shell__panel">
        <div class="auth-shell__card">
          <span class="auth-shell__eyebrow">{{ 'forgotPassword.eyebrow' | translate }}</span>
          <h2>{{ 'forgotPassword.formTitle' | translate }}</h2>
          <p>{{ 'forgotPassword.supporting' | translate }}</p>

          @if (successMessage) {
            <div class="auth-shell__success" role="status">
              {{ successMessage }}
              <a routerLink="/login">{{ 'forgotPassword.loginLink' | translate }}</a>
            </div>
          }

          <form
            class="auth-shell__form"
            [formGroup]="resetRequestForm"
            (ngSubmit)="submit()"
            [attr.aria-label]="'forgotPassword.formTitle' | translate"
          >
            <label>
              <span>{{ 'login.email' | translate }}</span>
              <input type="email" formControlName="email" autocomplete="username" placeholder="name@example.com" />
              @if (emailError()) {
                <small class="field-error">{{ emailError() }}</small>
              }
            </label>
            <button type="submit" [disabled]="submitting">
              {{ submitting ? ('forgotPassword.submitting' | translate) : ('forgotPassword.action' | translate) }}
            </button>
          </form>
        </div>
      </section>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForgotPasswordPageComponent {
  private readonly authService = inject(AuthService);
  private static readonly GENERIC_SUCCESS =
    'If a matching active account exists, a password reset email has been sent.';

  protected readonly resetRequestForm = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
  });
  protected submitting = false;
  protected successMessage = '';

  protected submit(): void {
    if (this.resetRequestForm.invalid || this.submitting) {
      this.resetRequestForm.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.successMessage = '';

    this.authService
      .requestPasswordReset(this.resetRequestForm.getRawValue())
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (payload) => {
          this.successMessage = payload.message || ForgotPasswordPageComponent.GENERIC_SUCCESS;
          this.resetRequestForm.reset();
        },
        error: () => {
          this.successMessage = ForgotPasswordPageComponent.GENERIC_SUCCESS;
          this.resetRequestForm.reset();
        },
      });
  }

  protected emailError(): string | null {
    const control = this.resetRequestForm.controls.email;
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
}
