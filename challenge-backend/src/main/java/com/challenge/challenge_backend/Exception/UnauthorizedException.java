package com.challenge.challenge_backend.Exception;

/**
 * Excepción para errores de autenticación.
 * Se usa cuando las credenciales son inválidas o el token ha expirado.
 * 
 * HTTP Status: 401 UNAUTHORIZED
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}