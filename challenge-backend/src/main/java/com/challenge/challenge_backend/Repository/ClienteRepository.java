package com.challenge.challenge_backend.Repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.challenge.challenge_backend.Models.Cliente;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Buscar clientes por nombre
     */
    List<Cliente> findByNombre(String nombre);

    /**
     * Buscar clientes por apellido
     */
    List<Cliente> findByApellido(String apellido);

    /**
     * Buscar clientes por nombre Y apellido
     */
    Optional<Cliente> findByNombreAndApellido(String nombre, String apellido);

    /**
     * Obtener solo clientes activos
     */
    List<Cliente> findByActivoTrue();
    
    /**
     * Obtener clientes activos con paginación
     * ESTE ES EL MÉTODO QUE FALTABA
     */
    Page<Cliente> findByActivoTrue(Pageable pageable);

    /**
     * Obtener solo clientes inactivos
     */
    List<Cliente> findByActivoFalse();

    /**
     * Buscar cliente por ID solo si está activo
     */
    Optional<Cliente> findByIdAndActivoTrue(Long id);

    /**
     * Contar clientes activos
     */
    long countByActivoTrue();

    /**
     * Obtener estadísticas de edad de clientes activos
     */
    @Query("SELECT AVG(c.edad) FROM Cliente c WHERE c.activo = true")
    Double obtenerPromedioEdad();

    /**
     * Obtener la edad mínima de clientes activos
     */
    @Query("SELECT MIN(c.edad) FROM Cliente c WHERE c.activo = true")
    Integer obtenerEdadMinima();

    /**
     * Obtener la edad máxima de clientes activos
     */
    @Query("SELECT MAX(c.edad) FROM Cliente c WHERE c.activo = true")
    Integer obtenerEdadMaxima();

    /**
     * Buscar clientes por rango de edad
     */
    @Query("SELECT c FROM Cliente c WHERE c.edad BETWEEN :edadMin AND :edadMax AND c.activo = true")
    List<Cliente> findByRangoEdad(@Param("edadMin") Integer edadMin, @Param("edadMax") Integer edadMax);

    /**
     * Verificar si existe un cliente con el mismo nombre, apellido y fecha de nacimiento
     */
    boolean existsByNombreAndApellidoAndFechaNacimiento(String nombre, String apellido, LocalDate fechaNacimiento);

    /**
     * Buscar clientes que cumplan años hoy
     */
    @Query("SELECT c FROM Cliente c WHERE " +
            "MONTH(c.fechaNacimiento) = MONTH(CURRENT_DATE) " +
            "AND DAY(c.fechaNacimiento) = DAY(CURRENT_DATE) " +
            "AND c.activo = true")
    List<Cliente> findClientesCumpleañosHoy();

    /**
     * Obtener clientes ordenados por fecha de registro (más recientes primero)
     */
    List<Cliente> findByActivoTrueOrderByFechaRegistroDesc();

    /**
     * Buscar clientes cuyo nombre o apellido contenga un texto (para búsquedas)
     */
    List<Cliente> findByNombreContainingIgnoreCaseOrApellidoContainingIgnoreCase(String nombre, String apellido);

    /**
     * Obtener clientes adultos con límite usando Pageable
     */
    @Query("SELECT c FROM Cliente c WHERE c.edad >= 18 AND c.activo = true")
    List<Cliente> findClientesAdultosConLimite(Pageable pageable);
    
    /**
     * Alternativa: Obtener los primeros N clientes adultos activos
     */
    List<Cliente> findTop10ByEdadGreaterThanEqualAndActivoTrue(Integer edadMinima);
    
    /**
     * Otra alternativa: Usar Page para tener información de paginación
     */
    Page<Cliente> findByEdadGreaterThanEqualAndActivoTrue(Integer edad, Pageable pageable);
}