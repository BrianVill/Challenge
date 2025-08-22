package com.challenge.challenge_backend.DTOs.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO genérico para estandarizar todas las respuestas de la API.
 * 
 * Proporciona una estructura consistente para respuestas exitosas y errores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)  // No incluir campos null en el JSON
public class ApiResponseDTO<T> {
    
    /**
     * Indica si la operación fue exitosa
     */
    private boolean success;
    
    /**
     * Mensaje descriptivo sobre el resultado de la operación
     */
    private String message;
    
    /**
     * Los datos de la respuesta (puede ser un objeto, lista, etc.)
     */
    private T data;
    
    /**
     * Timestamp de cuando se generó la respuesta
     */
    private LocalDateTime timestamp;
    
    /**
     * Código de error (solo en caso de error)
     */
    private String errorCode;
    
    /**
     * Path del endpoint que generó la respuesta
     */
    private String path;
    
    /**
     * Crea una respuesta exitosa con datos
     */
    public static <T> ApiResponseDTO<T> success(T data) {
        return ApiResponseDTO.<T>builder()
                .success(true)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Crea una respuesta exitosa con datos y mensaje
     */
    public static <T> ApiResponseDTO<T> success(T data, String message) {
        return ApiResponseDTO.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Crea una respuesta exitosa solo con mensaje (sin datos)
     */
    public static ApiResponseDTO<Void> successMessage(String message) {
        return ApiResponseDTO.<Void>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Crea una respuesta de error
     */
    public static ApiResponseDTO<Void> error(String message, String errorCode) {
        return ApiResponseDTO.<Void>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Crea una respuesta de error con path
     */
    public static ApiResponseDTO<Void> error(String message, String errorCode, String path) {
        return ApiResponseDTO.<Void>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
