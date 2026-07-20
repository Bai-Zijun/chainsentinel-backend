package com.bzj.chainsentinel.exception;

import com.bzj.chainsentinel.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .findFirst()
                .orElse("invalid request parameter");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException exception) {
        String message = exception.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("invalid request parameter");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleArgumentNotValid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .findFirst()
                .orElse("invalid request body");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "invalid request parameter");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled request exception", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error");
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiResponse.fail(status.value(), message));
    }
}
