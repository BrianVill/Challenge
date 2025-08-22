package com.challenge.challenge_backend.Exception;

import java.util.Map;

/**
 * Excepción para errores de validación de datos.
 * Puede contener múltiples errores de validación por campo.
 * 
 * HTTP Status: 400 BAD REQUEST
 */
public class ValidationException extends RuntimeException {
    
    private Map<String, String> errors;
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
}
