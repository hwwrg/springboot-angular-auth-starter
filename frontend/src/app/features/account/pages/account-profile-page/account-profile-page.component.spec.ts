import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AccountProfilePageComponent } from './account-profile-page.component';

describe('AccountProfilePageComponent', () => {
  let fixture: ComponentFixture<AccountProfilePageComponent>;
  let authService: jasmine.SpyObj<
    Pick<
      AuthService,
      'changeOwnPassword' | 'userProfile' | 'currentOrganization' | 'roles' | 'session' | 'mfaStatus'
    >
  >;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<
      Pick<
        AuthService,
        'changeOwnPassword' | 'userProfile' | 'currentOrganization' | 'roles' | 'session' | 'mfaStatus'
      >
    >('AuthService', [
      'changeOwnPassword',
      'userProfile',
      'currentOrganization',
      'roles',
      'session',
      'mfaStatus',
    ]);
    authService.userProfile.and.returnValue(null);
    authService.currentOrganization.and.returnValue(null);
    authService.roles.and.returnValue(['SUPERADMIN']);
    authService.mfaStatus.and.returnValue(of({ enabled: false, pending: false, remainingRecoveryCodes: 0 }));
    authService.session.and.returnValue({
      authenticated: true,
      mustChangePassword: false,
      mfaRequired: false,
      principal: {
        id: 'baseline-operator',
        email: 'operator@authstarter.local',
        displayName: 'Baseline Operator',
        roles: ['SUPERADMIN'],
        mustChangePassword: false,
      },
    });

    await TestBed.configureTestingModule({
      imports: [AccountProfilePageComponent],
      providers: [{ provide: AuthService, useValue: authService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountProfilePageComponent);
    fixture.detectChanges();
  });

  it('renders an authenticated password-change entry point', () => {
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Password management');
    expect(fixture.nativeElement.querySelector('input[formcontrolname="currentPassword"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('input[formcontrolname="newPassword"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('input[formcontrolname="confirmPassword"]')).not.toBeNull();
  });

  it('toggles account password visibility without submitting the form', () => {
    const input = fixture.nativeElement.querySelector(
      'input[formcontrolname="newPassword"]',
    ) as HTMLInputElement;
    const toggles = Array.from(
      fixture.nativeElement.querySelectorAll('button[aria-label="Show password"]'),
    ) as HTMLButtonElement[];
    const newPasswordToggle = toggles[1];

    expect(input.type).toBe('password');
    expect(newPasswordToggle.type).toBe('button');

    newPasswordToggle.click();
    fixture.detectChanges();

    expect(input.type).toBe('text');
    expect(newPasswordToggle.getAttribute('aria-label')).toBe('Hide password');
    expect(authService.changeOwnPassword).not.toHaveBeenCalled();
  });

  it('validates backend password length before changing the password', () => {
    setInputValue('input[formcontrolname="currentPassword"]', 'current-db-backed-password');
    setInputValue('input[formcontrolname="newPassword"]', 'short');
    setInputValue('input[formcontrolname="confirmPassword"]', 'short');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.changeOwnPassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Password must be 12 to 128 characters.');
  });

  it('validates password confirmation before changing the password', () => {
    setInputValue('input[formcontrolname="currentPassword"]', 'current-db-backed-password');
    setInputValue('input[formcontrolname="newPassword"]', 'new-db-backed-password');
    setInputValue('input[formcontrolname="confirmPassword"]', 'different-db-backed-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.changeOwnPassword).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Passwords do not match.');
  });

  it('submits a valid own-password change and clears the form', () => {
    authService.changeOwnPassword.and.returnValue(
      of({
        authenticated: true,
        mustChangePassword: false,
        mfaRequired: false,
        principal: authService.session().principal,
      }),
    );
    setInputValue('input[formcontrolname="currentPassword"]', 'current-db-backed-password');
    setInputValue('input[formcontrolname="newPassword"]', 'new-db-backed-password');
    setInputValue('input[formcontrolname="confirmPassword"]', 'new-db-backed-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.changeOwnPassword).toHaveBeenCalledOnceWith({
      currentPassword: 'current-db-backed-password',
      newPassword: 'new-db-backed-password',
    });
    expect(fixture.nativeElement.textContent).toContain('Password changed.');
  });

  it('shows a safe error when password change fails', () => {
    authService.changeOwnPassword.and.returnValue(throwError(() => new Error('Password does not meet policy.')));
    setInputValue('input[formcontrolname="currentPassword"]', 'current-db-backed-password');
    setInputValue('input[formcontrolname="newPassword"]', 'new-db-backed-password');
    setInputValue('input[formcontrolname="confirmPassword"]', 'new-db-backed-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Password does not meet policy.');
    expect(fixture.nativeElement.textContent).not.toContain('new-db-backed-password');
    const submit = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(submit.disabled).toBeFalse();
    expect(submit.textContent?.trim()).toBe('Change password');
  });

  function setInputValue(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }
});
