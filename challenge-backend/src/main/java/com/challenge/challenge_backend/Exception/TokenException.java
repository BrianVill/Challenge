package com.challenge.challenge_backend.Exception;

/**
 * Excepción específica para errores relacionados con JWT.
 * 
 * HTTP Status: 401 UNAUTHORIZED
 */
public class TokenException extends RuntimeException {
    
    public enum TokenErrorType {
        EXPIRED,
        INVALID,
        MALFORMED,
        UNSUPPORTED,
        SIGNATURE_INVALID
    }
    
    private TokenErrorType errorType;
    
    public TokenException(String message) {
        super(message);
    }
    
    public TokenException(String message, TokenErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }
    
    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public TokenErrorType getErrorType() {
        return errorType;
    }
}
