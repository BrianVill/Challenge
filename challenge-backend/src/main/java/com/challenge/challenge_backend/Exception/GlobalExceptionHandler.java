package com.challenge.challenge_backend.Exception;

import com.challenge.challenge_backend.DTOs.Response.ApiResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para toda la aplicación.
 * 
 * Centraliza el manejo de errores y genera respuestas consistentes
 * para todas las excepciones.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    // ========================================
    // EXCEPCIONES DE NEGOCIO
    // ========================================
    
    /**
     * Maneja BusinessException - Errores de lógica de negocio.
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {
        
        log.error("Error de negocio: {}", ex.getMessage());
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            ex.getMessage(),
            ex.getErrorCode() != null ? ex.getErrorCode() : "BUSINESS_ERROR",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Maneja ResourceNotFoundException - Recurso no encontrado.
     * HTTP 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        log.error("Recurso no encontrado: {}", ex.getMessage());
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            ex.getMessage(),
            "RESOURCE_NOT_FOUND",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Maneja DuplicateResourceException - Recurso duplicado.
     * HTTP 409 Conflict
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleDuplicateResourceException(
            DuplicateResourceException ex,
            HttpServletRequest request) {
        
        log.error("Recurso duplicado: {}", ex.getMessage());
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            ex.getMessage(),
            "DUPLICATE_RESOURCE",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    // ========================================
    // EXCEPCIONES DE VALIDACIÓN
    // ========================================
    
    /**
     * Maneja errores de validación de @Valid.
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Errores de validación: {}", errors);
        
        ApiResponseDTO<Map<String, String>> response = ApiResponseDTO.<Map<String, String>>builder()
            .success(false)
            .message("Error de validación en los datos enviados")
            .data(errors)
            .errorCode("VALIDATION_ERROR")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Maneja ValidationException personalizada.
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponseDTO<Map<String, String>>> handleCustomValidationException(
            ValidationException ex,
            HttpServletRequest request) {
        
        log.error("Error de validación: {}", ex.getMessage());
        
        ApiResponseDTO<Map<String, String>> response = ApiResponseDTO.<Map<String, String>>builder()
            .success(false)
            .message(ex.getMessage())
            .data(ex.getErrors())
            .errorCode("VALIDATION_ERROR")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    // ========================================
    // EXCEPCIONES DE SEGURIDAD
    // ========================================
    
    /**
     * Maneja UnauthorizedException - No autenticado.
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleUnauthorizedException(
            UnauthorizedException ex,
            HttpServletRequest request) {
        
        log.error("Error de autenticación: {}", ex.getMessage());
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            ex.getMessage(),
            "UNAUTHORIZED",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Maneja ForbiddenException - Sin permisos.
     * HTTP 403 Forbidden
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleForbiddenException(
            ForbiddenException ex,
            HttpServletRequest request) {
        
        log.error("Error de autorización: {}", ex.getMessage());
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            ex.getMessage(),
            "FORBIDDEN",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Maneja TokenException - Errores de JWT.
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleTokenException(
            TokenException ex,
            HttpServletRequest request) {
        
        log.error("Error de token: {}", ex.getMessage());
        
        String errorCode = ex.getErrorType() != null ? 
            "TOKEN_" + ex.getErrorType().name() : "TOKEN_ERROR";
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            ex.getMessage(),
            errorCode,
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Maneja errores de autenticación de Spring Security.
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        
        log.error("Error de autenticación: {}", ex.getMessage());
        
        String message = "Error de autenticación";
        if (ex instanceof BadCredentialsException) {
            message = "Credenciales inválidas";
        }
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            message,
            "AUTHENTICATION_ERROR",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Maneja errores de acceso denegado de Spring Security.
     * HTTP 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        
        log.error("Acceso denegado: {}", ex.getMessage());
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            "No tienes permisos para realizar esta acción",
            "ACCESS_DENIED",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    // ========================================
    // EXCEPCIONES GENERALES
    // ========================================
    
    /**
     * Maneja errores de tipo de argumento incorrecto.
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        String message = String.format("El parámetro '%s' tiene un valor inválido: '%s'",
            ex.getName(), ex.getValue());
        
        log.error("Error de tipo de argumento: {}", message);
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            message,
            "INVALID_ARGUMENT",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Maneja IllegalArgumentException.
     * HTTP 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        log.error("Argumento ilegal: {}", ex.getMessage());
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            ex.getMessage(),
            "ILLEGAL_ARGUMENT",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Maneja cualquier excepción no controlada.
     * HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleGlobalException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Error no controlado: ", ex);
        
        ApiResponseDTO<Void> response = ApiResponseDTO.error(
            "Ha ocurrido un error interno en el servidor",
            "INTERNAL_SERVER_ERROR",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
