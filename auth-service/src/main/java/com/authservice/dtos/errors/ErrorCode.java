package com.authservice.dtos.errors;

public enum ErrorCode {
    AUTH_INVALID_CREDENTIALS,
    AUTH_USER_ALREADY_EXISTS,

    VALIDATION_FAILED,
    INTERNAL_SERVER_ERROR
}