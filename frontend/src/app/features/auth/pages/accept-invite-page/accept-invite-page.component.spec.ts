import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AcceptInvitePageComponent } from './accept-invite-page.component';

describe('AcceptInvitePageComponent', () => {
  let fixture: ComponentFixture<AcceptInvitePageComponent>;
  let authService: jasmine.SpyObj<Pick<AuthService, 'acceptUserInvite'>>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<Pick<AuthService, 'acceptUserInvite'>>('AuthService', [
      'acceptUserInvite',
    ]);
  });

  it('shows a safe error when the invitation token is missing', async () => {
    await setupComponent({});

    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Invitation token is missing.');
    expect(authService.acceptUserInvite).not.toHaveBeenCalled();
  });

  it('validates backend password length before accepting the invite', async () => {
    await setupComponent({ token: 'opaque-token' });
    setInputValue('input[formcontrolname="newPassword"]', 'short');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.acceptUserInvite).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Password must be 12 to 128 characters.');
  });

  it('toggles invite password visibility without submitting the form', async () => {
    await setupComponent({ token: 'opaque-token' });
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
    expect(authService.acceptUserInvite).not.toHaveBeenCalled();
  });

  it('submits the invite token without displaying it', async () => {
    authService.acceptUserInvite.and.returnValue(
      of({
        email: 'invited-user@example.test',
        status: 'ACTIVE',
      }),
    );
    await setupComponent({ token: 'opaque-token' });
    setInputValue('input[formcontrolname="newPassword"]', 'new-db-backed-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(authService.acceptUserInvite).toHaveBeenCalledOnceWith({
      token: 'opaque-token',
      newPassword: 'new-db-backed-password',
    });
    expect(fixture.nativeElement.textContent).toContain('Password setup is complete.');
    expect(fixture.nativeElement.textContent).not.toContain('opaque-token');
  });

  it('shows a safe GraphQL error after invite acceptance fails', async () => {
    authService.acceptUserInvite.and.returnValue(throwError(() => new Error('Invitation has expired.')));
    await setupComponent({ token: 'opaque-token' });
    setInputValue('input[formcontrolname="newPassword"]', 'new-db-backed-password');

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Invitation has expired.');
    expect(fixture.nativeElement.textContent).not.toContain('opaque-token');
    const submit = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(submit.disabled).toBeFalse();
    expect(submit.textContent?.trim()).toBe('Set password');
  });

  async function setupComponent(queryParams: Record<string, string>): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [AcceptInvitePageComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(queryParams),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AcceptInvitePageComponent);
    fixture.detectChanges();
  }

  function setInputValue(selector: string, value: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }
});
