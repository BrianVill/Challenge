package com.challenge.challenge_backend.DTOs.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para enviar las estadísticas de los clientes.
 * 
 * Incluye el promedio de edad y desviación estándar
 * según los requerimientos del challenge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticasClienteDTO {
    
    /**
     * Total de clientes activos en el sistema
     */
    private Long totalClientes;
    
    /**
     * Promedio de edad de todos los clientes activos
     * Formateado a 2 decimales
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT, pattern = "#.##")
    private Double promedioEdad;
    
    /**
     * Desviación estándar de las edades
     * Formateado a 2 decimales
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT, pattern = "#.##")
    private Double desviacionEstandar;
    
    /**
     * Edad mínima entre todos los clientes
     */
    private Integer edadMinima;
    
    /**
     * Edad máxima entre todos los clientes
     */
    private Integer edadMaxima;
    
    /**
     * Mediana de las edades
     */
    private Double medianaEdad;
    
    /**
     * Distribución de clientes por rango de edad
     * Ej: {"0-17": 5, "18-29": 15, "30-44": 20, ...}
     */
    private Map<String, Long> distribucionPorRangoEdad;
    
    /**
     * Fecha y hora cuando se calcularon las estadísticas
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCalculo;
    
    /**
     * Mensaje descriptivo sobre las estadísticas
     */
    private String mensaje;
    
    /**
     * Constructor simplificado para los campos requeridos
     */
    public EstadisticasClienteDTO(Long totalClientes, Double promedioEdad, Double desviacionEstandar) {
        this.totalClientes = totalClientes;
        this.promedioEdad = Math.round(promedioEdad * 100.0) / 100.0;  // Redondear a 2 decimales
        this.desviacionEstandar = Math.round(desviacionEstandar * 100.0) / 100.0;
        this.fechaCalculo = LocalDateTime.now();
        this.mensaje = String.format("Estadísticas calculadas para % de clientes", totalClientes);
    }
}
