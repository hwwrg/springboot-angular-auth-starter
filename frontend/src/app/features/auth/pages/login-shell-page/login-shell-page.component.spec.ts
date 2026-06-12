import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/auth/auth.service';
import { RuntimeConfigService } from '../../../../core/runtime-config/runtime-config.service';
import { LoginShellPageComponent } from './login-shell-page.component';

type AuthServiceSpyMethods = Pick<
  AuthService,
  | 'login'
  | 'verifyMfa'
  | 'changeOwnPassword'
  | 'mustChangePassword'
  | 'fetchOAuth2Providers'
  | 'oauth2AuthorizationUrl'
>;

describe('LoginShellPageComponent', () => {
  let fixture: ComponentFixture<LoginShellPageComponent>;
  let authService: jasmine.SpyObj<AuthServiceSpyMethods>;
  let runtimeConfigService: jasmine.SpyObj<Pick<RuntimeConfigService, 'config'>>;
  let router: Router;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthServiceSpyMethods>('AuthService', [
      'login',
      'verifyMfa',
      'changeOwnPassword',
      'mustChangePassword',
      'fetchOAuth2Providers',
      'oauth2AuthorizationUrl',
    ]);
    authService.mustChangePassword.and.returnValue(false);
    authService.fetchOAuth2Providers.and.returnValue(of([]));
    runtimeConfigService = jasmine.createSpyObj<Pick<RuntimeConfigService, 'config'>>(
      'RuntimeConfigService',
      ['config'],
    );
    runtimeConfigService.config.and.returnValue(environment);

    await TestBed.configureTestingModule({
      imports: [LoginShellPageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: RuntimeConfigService, useValue: runtimeConfigService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({}),
            },
          },
        },
      ],
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture = TestBed.createComponent(LoginShellPageComponent);
    fixture.detectChanges();
  });

  it('shows the application version on the visual panel', () => {
    expect(fixture.nativeElement.textContent).toContain('Version 0.0.0');
  });

  it('renders a single sign-on button for each configured OAuth2 provider', () => {
    authService.fetchOAuth2Providers.and.returnValue(
      of([
        { id: 'google', label: 'Google', authorizationUrl: '/oauth2/authorization/google' },
        { id: 'github', label: 'GitHub', authorizationUrl: '/oauth2/authorization/github' },
      ]),
    );

    fixture = TestBed.createComponent(LoginShellPageComponent);
    fixture.detectChanges();

    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('.auth-shell__sso-button') as NodeListOf<HTMLButtonElement>,
    );
    expect(buttons.map((button) => button.textContent?.trim())).toEqual([
      'Continue with Google',
      'Continue with GitHub',
    ]);
  });

  it('hides the single sign-on section when no OAuth2 provider is configured', () => {
    expect(fixture.nativeElement.querySelector('.auth-shell__sso')).toBeNull();
  });

  it('shows an explanation when an OAuth2 login could not be linked to an account', () => {
    const route = TestBed.inject(ActivatedRoute);
    (route.snapshot as { queryParamMap: ReturnType<typeof convertToParamMap> }).queryParamMap =
      convertToParamMap({ error: 'oauth2-unlinked' });

    fixture = TestBed.createComponent(LoginShellPageComponent);
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    expect(alert?.textContent).toContain('No active account matches the identity provider email.');
  });

  it('shows a forgot-password link on the login form', () => {
    const link = fixture.nativeElement.querySelector('a[routerlink="/forgot-password"]') as HTMLAnchorElement | null;

    expect(link).not.toBeNull();
    expect(link?.textContent?.trim()).toBe('Forgot password?');
  });

  it('shows the runtime-config application version when provided', () => {
    runtimeConfigService.config.and.returnValue({
      ...environment,
      appVersion: '0.0.0-8f90177',
    });

    fixture = TestBed.createComponent(LoginShellPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Version 0.0.0-8f90177');
  });

  it('shows an alert and recovers the submit button after invalid credentials', () => {
    authService.login.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' })),
    );
    setInputValue('input[formcontrolname="email"]', 'name@example.com');
    setInputValue('input[formcontrolname="password"]', 'wrong-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    const button = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;

    expect(alert?.textContent?.trim()).toBe('Invalid email or password.');
    expect(button.disabled).toBeFalse();
    expect(button.textContent?.trim()).toBe('Sign in');
  });

  it('toggles login password visibility without submitting the form', () => {
    const input = fixture.nativeElement.querySelector(
      'input[formcontrolname="password"]',
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
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('navigates to the default app route after valid login', () => {
    authService.login.and.returnValue(
      of({
        authenticated: true,
        mustChangePassword: false,
        mfaRequired: false,
        principal: {
          id: 'baseline-operator',
          email: 'name@example.com',
          displayName: 'Baseline Operator',
          roles: ['SUPERADMIN'],
          mustChangePassword: false,
        },
      }),
    );
    setInputValue('input[formcontrolname="email"]', 'name@example.com');
    setInputValue('input[formcontrolname="password"]', 'password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));

    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/app/dashboard');
  });

  it('shows the verification step instead of navigating when a second factor is required', () => {
    authService.login.and.returnValue(
      of({ authenticated: false, mustChangePassword: false, mfaRequired: true, principal: null }),
    );
    setInputValue('input[formcontrolname="email"]', 'mfa@example.test');
    setInputValue('input[formcontrolname="password"]', 'correct-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('input[formcontrolname="code"]')).not.toBeNull();
  });

  it('verifies the second factor and navigates to the app on success', () => {
    authService.login.and.returnValue(
      of({ authenticated: false, mustChangePassword: false, mfaRequired: true, principal: null }),
    );
    authService.verifyMfa.and.returnValue(
      of({
        authenticated: true,
        mustChangePassword: false,
        mfaRequired: false,
        principal: {
          id: '30000000-0000-4000-8000-000000000099',
          email: 'mfa@example.test',
          displayName: 'MFA User',
          roles: ['USER'],
          mustChangePassword: false,
        },
      }),
    );
    setInputValue('input[formcontrolname="email"]', 'mfa@example.test');
    setInputValue('input[formcontrolname="password"]', 'correct-password');
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    setInputValue('input[formcontrolname="code"]', '123456');
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    expect(authService.verifyMfa).toHaveBeenCalledOnceWith({ code: '123456' });
    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/app/dashboard');
  });

  it('shows the new password form instead of navigating when password change is required', () => {
    authService.login.and.returnValue(
      of({
        authenticated: true,
        mustChangePassword: true,
        mfaRequired: false,
        principal: {
          id: '30000000-0000-4000-8000-000000000099',
          email: 'recovery@example.test',
          displayName: 'Recovery Operator',
          roles: ['SUPERADMIN'],
          mustChangePassword: true,
        },
      }),
    );
    setInputValue('input[formcontrolname="email"]', 'recovery@example.test');
    setInputValue('input[formcontrolname="password"]', 'break-glass-password');
    authService.mustChangePassword.and.returnValue(true);

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('input[formcontrolname="newPassword"]')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Set password');
  });

  it('submits the new password and navigates after password change success', () => {
    authService.mustChangePassword.and.returnValue(true);
    authService.changeOwnPassword.and.returnValue(
      of({
        authenticated: true,
        mustChangePassword: false,
        mfaRequired: false,
        principal: {
          id: '30000000-0000-4000-8000-000000000099',
          email: 'recovery@example.test',
          displayName: 'Recovery Operator',
          roles: ['SUPERADMIN'],
          mustChangePassword: false,
        },
      }),
    );
    fixture = TestBed.createComponent(LoginShellPageComponent);
    fixture.detectChanges();
    setInputValue('input[formcontrolname="newPassword"]', 'new-db-backed-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));

    expect(authService.changeOwnPassword).toHaveBeenCalledOnceWith({
      newPassword: 'new-db-backed-password',
    });
    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/app/dashboard');
  });

  it('toggles forced password-change visibility without submitting the form', () => {
    authService.mustChangePassword.and.returnValue(true);
    fixture = TestBed.createComponent(LoginShellPageComponent);
    fixture.detectChanges();
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
    expect(authService.changeOwnPassword).not.toHaveBeenCalled();
  });

  it('shows forced password-change failures and recovers the submit button', () => {
    authService.mustChangePassword.and.returnValue(true);
    authService.changeOwnPassword.and.returnValue(throwError(() => new Error('Password does not meet policy.')));
    fixture = TestBed.createComponent(LoginShellPageComponent);
    fixture.detectChanges();
    setInputValue('input[formcontrolname="newPassword"]', 'new-db-backed-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement | null;
    const submit = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(alert?.textContent).toContain('Password does not meet policy.');
    expect(submit.disabled).toBeFalse();
    expect(submit.textContent?.trim()).toBe('Set password');
  });

  function setInputValue(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }
});
