package com.challenge.challenge_backend.DTOs.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuesta de autenticación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    
    /**
     * Token JWT para autenticación
     */
    private String token;
    
    /**
     * Tipo de token (generalmente "Bearer")
     */
    private String tokenType = "Bearer";
    
    /**
     * Tiempo de expiración en segundos
     */
    private Long expiresIn;
    
    /**
     * Email del usuario autenticado
     */
    private String email;
    
    /**
     * Nombre completo del usuario
     */
    private String nombreCompleto;
    
    /**
     * Rol del usuario (USER, ADMIN)
     */
    private String role;
    
    /**
     * Fecha y hora de cuando se generó el token
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAt;
    
    /**
     * Constructor conveniente para respuesta básica
     */
    public AuthResponseDTO(String token, Long expiresIn, String email, String nombre, String apellido, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.email = email;
        this.nombreCompleto = nombre + " " + apellido;
        this.role = role;
        this.issuedAt = LocalDateTime.now();
    }
}
