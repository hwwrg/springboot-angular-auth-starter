import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { ForgotPasswordPageComponent } from './forgot-password-page.component';

describe('ForgotPasswordPageComponent', () => {
  let fixture: ComponentFixture<ForgotPasswordPageComponent>;
  let authService: jasmine.SpyObj<Pick<AuthService, 'requestPasswordReset'>>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<Pick<AuthService, 'requestPasswordReset'>>(
      'AuthService',
      ['requestPasswordReset'],
    );

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordPageComponent);
    fixture.detectChanges();
  });

  it('submits an email and shows the generic success message', () => {
    authService.requestPasswordReset.and.returnValue(of({
      message: 'If a matching active account exists, a password reset email has been sent.',
    }));
    setInputValue('input[formcontrolname="email"]', 'operator@authstarter.local');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.requestPasswordReset).toHaveBeenCalledOnceWith({
      email: 'operator@authstarter.local',
    });
    expect(fixture.nativeElement.textContent).toContain(
      'If a matching active account exists, a password reset email has been sent.',
    );
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('shows the same generic success message when the backend request fails', () => {
    authService.requestPasswordReset.and.returnValue(throwError(() => new Error('Email provider failed.')));
    setInputValue('input[formcontrolname="email"]', 'operator@authstarter.local');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'If a matching active account exists, a password reset email has been sent.',
    );
    expect(fixture.nativeElement.textContent).not.toContain('Email provider failed.');
  });

  function setInputValue(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }
});
