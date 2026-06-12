import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { MfaEnrollment, MfaStatus } from '../../../../core/auth/auth.model';
import { OperationalErrorService } from '../../../../core/errors/operational-error.service';
import { TranslatePipe } from '../../../../core/i18n/translate.pipe';

type MfaView = 'idle' | 'enrolling' | 'recovery-codes';

@Component({
  selector: 'app-mfa-management',
  imports: [ReactiveFormsModule, TranslatePipe],
  template: `
    <article class="context-panel context-panel--wide">
      <div class="context-panel__header">
        <div>
          <p class="context-panel__eyebrow">{{ 'account.mfaEyebrow' | translate }}</p>
          <h2>{{ 'account.mfaTitle' | translate }}</h2>
        </div>
        @if (status(); as currentStatus) {
          <span class="status-pill" [class.status-pill--ok]="currentStatus.enabled">
            {{ (currentStatus.enabled ? 'account.mfaEnabled' : 'account.mfaDisabled') | translate }}
          </span>
        }
      </div>

      <p class="context-panel__empty">{{ 'account.mfaSupporting' | translate }}</p>

      @if (message()) {
        <div
          class="auth-shell__success account-password-message"
          [class.auth-shell__success--error]="messageTone() === 'error'"
          [attr.role]="messageTone() === 'error' ? 'alert' : 'status'"
        >
          {{ message() }}
        </div>
      }

      @if (view() === 'idle') {
        @if (status()?.enabled) {
          <p>{{ 'account.mfaRecoveryRemaining' | translate }}: {{ status()?.remainingRecoveryCodes }}</p>
          <button type="button" class="auth-shell__sso-button" [disabled]="busy()" (click)="disable()">
            {{ 'account.mfaDisableAction' | translate }}
          </button>
        } @else {
          <button type="button" [disabled]="busy()" (click)="startEnrollment()">
            {{ 'account.mfaEnableAction' | translate }}
          </button>
        }
      }

      @if (view() === 'enrolling' && enrollment(); as activeEnrollment) {
        <div class="mfa-enrollment">
          <p>{{ 'account.mfaScanInstruction' | translate }}</p>
          <p class="mfa-enrollment__secret">
            <span>{{ 'account.mfaSecret' | translate }}:</span>
            <code>{{ activeEnrollment.secret }}</code>
          </p>
          <p class="mfa-enrollment__uri"><code>{{ activeEnrollment.otpAuthUri }}</code></p>

          <form class="auth-shell__form" [formGroup]="confirmForm" (ngSubmit)="confirmEnrollment()">
            <label>
              <span>{{ 'account.mfaConfirmCode' | translate }}</span>
              <input formControlName="code" inputmode="numeric" autocomplete="one-time-code" placeholder="123456" />
            </label>
            <button type="submit" [disabled]="busy()">{{ 'account.mfaConfirmAction' | translate }}</button>
            <button type="button" class="auth-shell__sso-button" [disabled]="busy()" (click)="cancelEnrollment()">
              {{ 'account.mfaCancelAction' | translate }}
            </button>
          </form>
        </div>
      }

      @if (view() === 'recovery-codes') {
        <div class="mfa-recovery">
          <p>{{ 'account.mfaRecoveryIntro' | translate }}</p>
          <ul class="mfa-recovery__list">
            @for (code of recoveryCodes(); track code) {
              <li><code>{{ code }}</code></li>
            }
          </ul>
          <button type="button" (click)="acknowledgeRecoveryCodes()">{{ 'account.mfaRecoveryDone' | translate }}</button>
        </div>
      }
    </article>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MfaManagementComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly operationalErrorService = inject(OperationalErrorService);

  protected readonly status = signal<MfaStatus | null>(null);
  protected readonly enrollment = signal<MfaEnrollment | null>(null);
  protected readonly recoveryCodes = signal<string[]>([]);
  protected readonly view = signal<MfaView>('idle');
  protected readonly busy = signal(false);
  protected readonly message = signal('');
  protected readonly messageTone = signal<'success' | 'error'>('success');

  protected readonly confirmForm = new FormGroup({
    code: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  ngOnInit(): void {
    this.refreshStatus();
  }

  protected startEnrollment(): void {
    this.busy.set(true);
    this.clearMessage();
    this.authService
      .startMfaEnrollment()
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (enrollment) => {
          this.enrollment.set(enrollment);
          this.view.set('enrolling');
        },
        error: (error: unknown) => this.showError(error),
      });
  }

  protected confirmEnrollment(): void {
    if (this.confirmForm.invalid || this.busy()) {
      this.confirmForm.markAllAsTouched();
      return;
    }

    this.busy.set(true);
    this.clearMessage();
    this.authService
      .confirmMfaEnrollment({ code: this.confirmForm.getRawValue().code.trim() })
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (codes) => {
          this.recoveryCodes.set(codes.recoveryCodes);
          this.enrollment.set(null);
          this.confirmForm.reset();
          this.view.set('recovery-codes');
          this.refreshStatus();
        },
        error: (error: unknown) => this.showError(error),
      });
  }

  protected cancelEnrollment(): void {
    this.enrollment.set(null);
    this.confirmForm.reset();
    this.view.set('idle');
    this.clearMessage();
  }

  protected acknowledgeRecoveryCodes(): void {
    this.recoveryCodes.set([]);
    this.view.set('idle');
  }

  protected disable(): void {
    this.busy.set(true);
    this.clearMessage();
    this.authService
      .disableMfa()
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (status) => {
          this.status.set(status);
          this.view.set('idle');
          this.messageTone.set('success');
          this.message.set('Multi-factor authentication disabled.');
        },
        error: (error: unknown) => this.showError(error),
      });
  }

  private refreshStatus(): void {
    this.authService.mfaStatus().subscribe({
      next: (status) => this.status.set(status),
      error: () => this.status.set(null),
    });
  }

  private showError(error: unknown): void {
    this.messageTone.set('error');
    this.message.set(this.operationalErrorService.loginFailureMessage(error));
  }

  private clearMessage(): void {
    this.message.set('');
    this.messageTone.set('success');
  }
}
