package com.example.bankcards.exception;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Validation failed");
        body.put("details", ex.getBindingResult().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,Object>> badReq(IllegalArgumentException ex){
        Map<String,Object> b = new HashMap<>();
        b.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(b);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,Object>> constraint(org.springframework.dao.DataIntegrityViolationException ex){
        Map<String,Object> b = new HashMap<>();
        b.put("error","Data integrity violation");
        ex.getMostSpecificCause();
        b.put("detail", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(b);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String,Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String,Object> b = new HashMap<>();
        b.put("error", "Forbidden");
        b.put("detail", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(b);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,Object>> conflict(IllegalStateException ex){
        Map<String,Object> b = new HashMap<>();
        b.put("error","Conflict");
        b.put("detail", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(b); // 409
    }
}
