package com.c05.kaz.ecommercebackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bắt các RuntimeException mình chủ động throw trong service
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(Map.of(
                        "message", ex.getMessage()
                ));
    }

    // Có thể bổ sung handler khác nếu cần (ValidationException, etc.)
}
