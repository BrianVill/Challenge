package com.challenge.challenge_backend.DTOs.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para detalles de errores en operaciones batch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetalleDTO {
    private Integer indice;  // Posición en el array original
    private String nombre;
    private String apellido;
    private String error;
}
