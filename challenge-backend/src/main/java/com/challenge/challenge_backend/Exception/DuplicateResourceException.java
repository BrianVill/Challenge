package com.challenge.challenge_backend.Exception;

/**
 * Excepción cuando se intenta crear un recurso que ya existe.
 * 
 * HTTP Status: 409 CONFLICT
 */
public class DuplicateResourceException extends RuntimeException {
    
    private String resourceName;
    private String fieldName;
    private Object fieldValue;
    
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s ya existe con %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    public String getResourceName() {
        return resourceName;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Object getFieldValue() {
        return fieldValue;
    }
}
