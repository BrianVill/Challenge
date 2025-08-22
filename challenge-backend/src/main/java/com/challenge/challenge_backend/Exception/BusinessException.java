package com.challenge.challenge_backend.Exception;

/**
 * Excepción para errores de lógica de negocio.
 * Se usa cuando se violan reglas del negocio.
 * 
 * HTTP Status: 400 BAD REQUEST
 */
public class BusinessException extends RuntimeException {
    
    private String errorCode;
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
