package org.example.mainbackendservice.controller;

import org.example.mainbackendservice.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", "VALIDATION_FAILED");
        errorDetails.put("invalidFields", errors);

        ApiResponse<Object> response = new ApiResponse<>(
                "ERROR",
                "Ошибка валидации входящих параметров.",
                errorDetails,
                null
        );

        return ResponseEntity.status(400).body(response);
    }
}