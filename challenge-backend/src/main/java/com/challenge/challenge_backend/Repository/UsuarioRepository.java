package com.challenge.challenge_backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.challenge.challenge_backend.Models.Usuario;
import com.challenge.challenge_backend.Models.Usuario.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    /**
     * Buscar usuario por email (para login)
     */
    Optional<Usuario> findByEmail(String email);
    
    /**
     * Verificar si existe un usuario con ese email
     */
    boolean existsByEmail(String email);
    
    /**
     * Buscar solo usuarios activos
     */
    List<Usuario> findByActivoTrue();
    
    /**
     * Buscar usuario por email solo si está activo
     * No permitir login a usuarios inactivos
     */
    Optional<Usuario> findByEmailAndActivoTrue(String email);
    
    /**
     * Buscar usuarios por rol
     */
    List<Usuario> findByRole(Role role);
    
    /**
     * Buscar usuarios por rol y estado activo
     */
    List<Usuario> findByRoleAndActivoTrue(Role role);
    
    /**
     * Contar usuarios por rol
     */
    long countByRole(Role role);
    
    /**
     * Buscar usuarios registrados en los últimos X días
     */
    @Query("SELECT u FROM Usuario u WHERE u.fechaRegistro >= :fecha AND u.activo = true")
    List<Usuario> findUsuariosRegistradosDesde(@Param("fecha") LocalDateTime fecha);
    
    /**
     * Obtener todos los administradores activos
     */
    @Query("SELECT u FROM Usuario u WHERE u.role = 'ADMIN' AND u.activo = true")
    List<Usuario> findAdministradoresActivos();
    
    /**
     * Buscar usuarios por nombre o apellido (para búsquedas)
     */
    @Query("SELECT u FROM Usuario u WHERE " +
           "(LOWER(u.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
           "LOWER(u.apellido) LIKE LOWER(CONCAT('%', :busqueda, '%'))) " +
           "AND u.activo = true")
    List<Usuario> buscarUsuarios(@Param("busqueda") String busqueda);
    
    /**
     * Desactivar un usuario (soft delete)
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.activo = false WHERE u.id = :id")
    void desactivarUsuario(@Param("id") Long id);
    
    /**
     * Activar un usuario
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.activo = true WHERE u.id = :id")
    void activarUsuario(@Param("id") Long id);
    
    /**
     * Actualizar el rol de un usuario
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.role = :nuevoRole WHERE u.id = :id")
    void actualizarRole(@Param("id") Long id, @Param("nuevoRole") Role nuevoRole);
    
    /**
     * Obtener estadísticas de usuarios
     */
    @Query(value = "SELECT " +
           "COUNT(*) as total, " +
           "SUM(CASE WHEN activo = 1 THEN 1 ELSE 0 END) as activos, " +
           "SUM(CASE WHEN role = 'ADMIN' THEN 1 ELSE 0 END) as admins " +
           "FROM usuarios", 
           nativeQuery = true)
    Object[] obtenerEstadisticasUsuarios();
    
    /**
     * Buscar usuarios que no han sido actualizados en mucho tiempo
     */
    @Query(value = "SELECT * FROM usuarios WHERE " +
           "DATEDIFF(CURRENT_DATE, fecha_registro) > :dias " +
           "AND activo = 1", 
           nativeQuery = true)
    List<Usuario> findUsuariosInactivosPorDias(@Param("dias") int dias);
}
