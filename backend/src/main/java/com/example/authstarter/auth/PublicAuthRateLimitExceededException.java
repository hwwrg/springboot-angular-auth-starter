package com.example.authstarter.auth;

public class PublicAuthRateLimitExceededException extends RuntimeException {

    public PublicAuthRateLimitExceededException(String message) {
        super(message);
    }
}
