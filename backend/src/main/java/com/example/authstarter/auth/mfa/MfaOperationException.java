package com.example.authstarter.auth.mfa;

/** Raised for recoverable MFA operations such as an incorrect verification code. */
public class MfaOperationException extends RuntimeException {

    public MfaOperationException(String message) {
        super(message);
    }
}
