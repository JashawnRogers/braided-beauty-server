package com.braided_beauty.braided_beauty.exceptions;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private Map<String, Object> body(HttpStatus status, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("status", status.value());
        m.put("error", status.getReasonPhrase());
        m.put("message", message);
        return m;
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(body(status, message));
    }

    // --- Custom exceptions ------------------------------------------------

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex){
        return response(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({ DuplicateEntityException.class, ConflictException.class })
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex){
        return response(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex){
        return response(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // --- Validation / request errors ------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex){
        Map<String, Object> b = body(HttpStatus.BAD_REQUEST, "Validation failed");
        // Field-level errors
        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> {
                    Map<String, String> e = new LinkedHashMap<>();
                    e.put("field", err.getField());
                    e.put("message", Optional.ofNullable(err.getDefaultMessage()).orElse("Invalid value"));
                    return e;
                })
                .toList();
        b.put("errors", errors);
        return ResponseEntity.badRequest().body(b);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> b = body(HttpStatus.BAD_REQUEST, "Validation failed");
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(err -> {
                    Map<String, String> e = new LinkedHashMap<>();
                    e.put("field", String.valueOf(err.getPropertyPath()));
                    e.put("message", err.getMessage());
                    return e;
                })
                .toList();
        b.put("errors", errors);
        return ResponseEntity.badRequest().body(b);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, Optional.ofNullable(ex.getMessage()).orElse("Bad request"));
    }

    // --- Data layer conflicts (unique constraints, FKs, etc.) -----------------


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = Optional.ofNullable(ex.getRootCause())
                .map(Throwable::getMessage)
                .orElse("Data conflict");
        return response(HttpStatus.CONFLICT, msg);
    }

    // --- Catch all -------------------------------------------------------------


    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception ex){
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Something went wrong. Please try again later.");
    }
}
