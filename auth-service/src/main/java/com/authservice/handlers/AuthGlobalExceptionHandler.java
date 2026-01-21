package com.authservice.handlers;

import com.authservice.dtos.errors.ApiErrorResponse;
import com.authservice.dtos.errors.ErrorCode;
import com.authservice.exceptions.InvalidCredentialsException;
import com.authservice.exceptions.UserAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class AuthGlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String detailedMessage = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return fieldName + ": " + errorMessage;
                })
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", detailedMessage);

        ApiErrorResponse error = new ApiErrorResponse(
                "Validation failed: " + detailedMessage,
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        log.warn("Registration attempt failed: email already exists. Error: {} | URL: {}", ex.getMessage(), request.getRequestURI());

        ApiErrorResponse error = new ApiErrorResponse(
                ex.getMessage(),
                ErrorCode.AUTH_USER_ALREADY_EXISTS,
                HttpStatus.CONFLICT.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler({InvalidCredentialsException.class,
            BadCredentialsException.class,
            UsernameNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication failed from IP: {} | URL: {}", request.getRemoteAddr(), request.getRequestURI());
        ApiErrorResponse error = new ApiErrorResponse(
                "Invalid email or password",
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected internal server error at URL: {}", request.getRequestURI(), ex);

        ApiErrorResponse error = new ApiErrorResponse(
                ex.getMessage(),
                ErrorCode.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}