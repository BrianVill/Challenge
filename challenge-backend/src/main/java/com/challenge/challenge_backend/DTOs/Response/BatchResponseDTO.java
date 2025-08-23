package com.challenge.challenge_backend.DTOs.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para respuesta de operaciones batch.
 * Incluye detalles de éxitos y errores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponseDTO {
    
    private Integer total;
    private Integer exitosos;
    private Integer fallidos;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaProcesamiento;
    
    private List<ClienteResponseDTO> clientesCreados;
    private List<ErrorDetalleDTO> errores;
}
