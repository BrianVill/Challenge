package com.challenge.challenge_backend.DTOs.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Incluye el cálculo de fecha probable de fallecimiento
 * según lo solicitado en los requerimientos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDTO {
    
    /**
     * ID único del cliente
     */
    private Long id;
    
    /**
     * Nombre del cliente
     */
    private String nombre;
    
    /**
     * Apellido del cliente
     */
    private String apellido;
    
    /**
     * Edad actual del cliente
     */
    private Integer edad;
    
    /**
     * Fecha de nacimiento
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;
    
    /**
     * Fecha probable de fallecimiento
     * Calculada basándose en esperanza de vida promedio (75 años por defecto)
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaProbableFallecimiento;
    
    /**
     * Años restantes hasta la fecha probable de fallecimiento
     */
    private Integer añosRestantes;
    
    /**
     * Días restantes hasta la fecha probable de fallecimiento
     */
    private Long diasRestantes;
    
    /**
     * Fecha cuando se registró el cliente
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaRegistro;
    
    /**
     * Método para calcular los campos derivados
     * Se debe llamar después de establecer fechaProbableFallecimiento
     */
    public void calcularCamposDerivados() {
        if (fechaProbableFallecimiento != null) {
            LocalDate hoy = LocalDate.now();
            
            // Calcular días restantes
            this.diasRestantes = ChronoUnit.DAYS.between(hoy, fechaProbableFallecimiento);
            
            // Si la fecha ya pasó, establecer en 0
            if (this.diasRestantes < 0) {
                this.diasRestantes = 0L;
                this.añosRestantes = 0;
            } else {
                // Calcular años restantes
                this.añosRestantes = (int) ChronoUnit.YEARS.between(hoy, fechaProbableFallecimiento);
            }
        }
    }
}
