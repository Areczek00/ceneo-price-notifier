package com.authservice.dtos.errors;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String message,
        ErrorCode code,
        int statusCode,
        LocalDateTime timestamp
) {}