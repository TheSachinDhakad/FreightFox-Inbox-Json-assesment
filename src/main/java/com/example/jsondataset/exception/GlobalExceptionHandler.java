package com.example.jsondataset.exception;

import com.example.jsondataset.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public org.springframework.http.ResponseEntity<ApiError> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public org.springframework.http.ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public org.springframework.http.ResponseEntity<ApiError> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Request body is missing or contains invalid JSON.",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DataAccessException.class)
    public org.springframework.http.ResponseEntity<ApiError> handleDatabase(
            DataAccessException ex, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                "Database operation failed. Check the database connection and server logs.",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ApiError> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.",
                request.getRequestURI(), Map.of());
    }

    private org.springframework.http.ResponseEntity<ApiError> response(
            HttpStatus status, String message, String path, Map<String, String> validationErrors) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, path, validationErrors);
        return org.springframework.http.ResponseEntity.status(status).body(body);
    }
}
