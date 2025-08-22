package com.challenge.challenge_backend.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Data
@Builder
@NoArgsConstructor // (requerido por JPA)
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "edad", nullable = false)
    private Integer edad;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    /**
     * Fecha y hora en que se registró el cliente en el sistema
     * Se establece automáticamente al crear el registro
     */
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    /**
     * Fecha y hora de la última actualización del registro
     * Se actualiza automáticamente cuando se modifica el registro
     */
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    /**
     * Indica si el cliente está activo en el sistema
     * Se usa para soft delete (no eliminar físicamente)
     * Por defecto es true
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Método que se ejecuta automáticamente ANTES de insertar
     * un nuevo registro en la base de datos
     */
    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
        if (activo == null) {
            activo = true;
        }
    }

    /**
     * Método que se ejecuta automáticamente ANTES de actualizar
     * un registro existente en la base de datos
     */
    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}