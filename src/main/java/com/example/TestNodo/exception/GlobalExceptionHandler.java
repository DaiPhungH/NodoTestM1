package com.example.TestNodo.exception;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation failed");

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            // Get localized field name
            String fieldKey = "field.product." + error.getField();
            String fieldName;
            try {
                fieldName = messageSource.getMessage(fieldKey, null, LocaleContextHolder.getLocale());
            } catch (Exception e) {
                fieldName = error.getField(); // Fallback to default field name
            }

            // Get localized error message
            String messageKey = error.getDefaultMessage();
            String message;
            try {
                if (messageKey != null && messageKey.startsWith("{") && messageKey.endsWith("}")) {
                    message = messageSource.getMessage(messageKey.substring(1, messageKey.length() - 1), null, LocaleContextHolder.getLocale());
                } else {
                    message = messageKey != null ? messageKey : "Invalid data";
                }
            } catch (Exception e) {
                message = messageKey != null ? messageKey : "Invalid data";
            }
            errors.put(fieldName, message);
        }
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation failed");

        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            String fieldKey = "field.product." + field;
            String fieldName;
            try {
                fieldName = messageSource.getMessage(fieldKey, null, LocaleContextHolder.getLocale());
            } catch (Exception e) {
                fieldName = field; // Fallback to default field name
            }

            String messageKey = violation.getMessage();
            String message;
            try {
                if (messageKey.startsWith("{") && messageKey.endsWith("}")) {
                    message = messageSource.getMessage(messageKey.substring(1, messageKey.length() - 1), null, LocaleContextHolder.getLocale());
                } else {
                    message = messageKey;
                }
            } catch (Exception e) {
                message = messageKey;
            }
            errors.put(fieldName, message);
        }
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(EntityNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "An unexpected error occurred: " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
