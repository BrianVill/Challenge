package com.challenge.challenge_backend.DTOs.Request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequestDTO {
    
    /**
     * Nombre del cliente
     * - Obligatorio
     * - Entre 2 y 100 caracteres
     * - Solo letras y espacios
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", 
             message = "El nombre solo puede contener letras y espacios")
    private String nombre;
    
    /**
     * Apellido del cliente
     * - Obligatorio
     * - Entre 2 y 100 caracteres
     * - Solo letras y espacios
     */
    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", 
             message = "El apellido solo puede contener letras y espacios")
    private String apellido;
    
    /**
     * Edad del cliente
     * - Obligatorio
     * - Entre 0 y 150 años
     */
    @NotNull(message = "La edad es obligatoria")
    @Min(value = 0, message = "La edad no puede ser negativa")
    @Max(value = 150, message = "La edad no puede ser mayor a 150 años")
    private Integer edad;
    
    /**
     * Fecha de nacimiento del cliente
     * - Obligatorio
     * - Debe ser una fecha pasada
     * - Se validará que sea coherente con la edad
     */
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    private LocalDate fechaNacimiento;
}