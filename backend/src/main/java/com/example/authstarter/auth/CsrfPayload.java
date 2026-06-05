package com.example.authstarter.auth;

public record CsrfPayload(String headerName, String parameterName, String token) {
}
