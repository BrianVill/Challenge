package com.challenge.challenge_backend.Exception;

/**
 * Excepción para errores de autorización.
 * Se usa cuando el usuario no tiene permisos para realizar una acción.
 * 
 * HTTP Status: 403 FORBIDDEN
 */
public class ForbiddenException extends RuntimeException {
    
    public ForbiddenException(String message) {
        super(message);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
