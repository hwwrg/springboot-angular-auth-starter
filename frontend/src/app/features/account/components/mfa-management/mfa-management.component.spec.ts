import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { MfaManagementComponent } from './mfa-management.component';

type MfaSpyMethods = Pick<
  AuthService,
  'mfaStatus' | 'startMfaEnrollment' | 'confirmMfaEnrollment' | 'disableMfa'
>;

describe('MfaManagementComponent', () => {
  let fixture: ComponentFixture<MfaManagementComponent>;
  let authService: jasmine.SpyObj<MfaSpyMethods>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<MfaSpyMethods>('AuthService', [
      'mfaStatus',
      'startMfaEnrollment',
      'confirmMfaEnrollment',
      'disableMfa',
    ]);
    authService.mfaStatus.and.returnValue(of({ enabled: false, pending: false, remainingRecoveryCodes: 0 }));

    await TestBed.configureTestingModule({
      imports: [MfaManagementComponent],
      providers: [{ provide: AuthService, useValue: authService }],
    }).compileComponents();

    fixture = TestBed.createComponent(MfaManagementComponent);
    fixture.detectChanges();
  });

  it('offers to enable MFA when it is disabled', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Enable two-factor authentication');
  });

  it('walks through enrollment and reveals recovery codes once confirmed', () => {
    authService.startMfaEnrollment.and.returnValue(
      of({ secret: 'JBSWY3DPEHPK3PXP', otpAuthUri: 'otpauth://totp/Acme:user' }),
    );
    authService.confirmMfaEnrollment.and.returnValue(of({ recoveryCodes: ['AAAA-BBBB-CCCC', 'DDDD-EEEE-FFFF'] }));

    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('JBSWY3DPEHPK3PXP');
    const codeInput = fixture.nativeElement.querySelector('input[formcontrolname="code"]') as HTMLInputElement;
    codeInput.value = '123456';
    codeInput.dispatchEvent(new Event('input'));
    (fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(authService.confirmMfaEnrollment).toHaveBeenCalledOnceWith({ code: '123456' });
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('AAAA-BBBB-CCCC');
    expect(text).toContain('DDDD-EEEE-FFFF');
  });

  it('disables MFA when it is already enabled', () => {
    authService.mfaStatus.and.returnValue(of({ enabled: true, pending: false, remainingRecoveryCodes: 8 }));
    authService.disableMfa.and.returnValue(of({ enabled: false, pending: false, remainingRecoveryCodes: 0 }));

    fixture = TestBed.createComponent(MfaManagementComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Disable two-factor authentication');
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(authService.disableMfa).toHaveBeenCalledTimes(1);
  });
});
