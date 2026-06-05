import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { ResetPasswordPageComponent } from './reset-password-page.component';

describe('ResetPasswordPageComponent', () => {
  let fixture: ComponentFixture<ResetPasswordPageComponent>;
  let authService: jasmine.SpyObj<Pick<AuthService, 'resetPassword'>>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<Pick<AuthService, 'resetPassword'>>('AuthService', ['resetPassword']);

    await TestBed.configureTestingModule({
      imports: [ResetPasswordPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ token: 'opaque-reset-token' }),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetPasswordPageComponent);
    fixture.detectChanges();
  });

  it('validates matching passwords before calling the backend', () => {
    setInputValue('input[formcontrolname="newPassword"]', 'new-db-backed-password');
    setInputValue('input[formcontrolname="confirmPassword"]', 'different-db-backed-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.resetPassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Passwords do not match.');
  });

  it('calls the backend mutation and does not display the raw token', () => {
    authService.resetPassword.and.returnValue(of({
      message: 'Password has been reset. You can now sign in.',
    }));
    setInputValue('input[formcontrolname="newPassword"]', 'new-db-backed-password');
    setInputValue('input[formcontrolname="confirmPassword"]', 'new-db-backed-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.resetPassword).toHaveBeenCalledOnceWith({
      token: 'opaque-reset-token',
      newPassword: 'new-db-backed-password',
    });
    expect(fixture.nativeElement.textContent).toContain('Password has been reset. You can now sign in.');
    expect(fixture.nativeElement.textContent).not.toContain('opaque-reset-token');
  });

  it('toggles password visibility without submitting the form', () => {
    const input = fixture.nativeElement.querySelector(
      'input[formcontrolname="newPassword"]',
    ) as HTMLInputElement;
    const toggle = fixture.nativeElement.querySelector(
      'button[aria-label="Show password"]',
    ) as HTMLButtonElement;

    expect(input.type).toBe('password');
    expect(toggle.type).toBe('button');

    toggle.click();
    fixture.detectChanges();

    expect(input.type).toBe('text');
    expect(toggle.getAttribute('aria-label')).toBe('Hide password');
    expect(authService.resetPassword).not.toHaveBeenCalled();
  });

  it('shows a safe error when reset fails', () => {
    authService.resetPassword.and.returnValue(throwError(() => new Error('Password reset token is invalid or expired.')));
    setInputValue('input[formcontrolname="newPassword"]', 'new-db-backed-password');
    setInputValue('input[formcontrolname="confirmPassword"]', 'new-db-backed-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Password reset token is invalid or expired.');
    expect(fixture.nativeElement.textContent).not.toContain('new-db-backed-password');
  });

  function setInputValue(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }
});
