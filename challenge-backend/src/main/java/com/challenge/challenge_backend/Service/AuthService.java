package com.challenge.challenge_backend.Service;

import com.challenge.challenge_backend.DTOs.Request.ChangePasswordRequestDTO;
import com.challenge.challenge_backend.DTOs.Request.LoginRequestDTO;
import com.challenge.challenge_backend.DTOs.Request.RegisterRequestDTO;
import com.challenge.challenge_backend.DTOs.Response.AuthResponseDTO;
import com.challenge.challenge_backend.Exception.BusinessException;
import com.challenge.challenge_backend.Exception.ForbiddenException;
import com.challenge.challenge_backend.Exception.UnauthorizedException;
import com.challenge.challenge_backend.Models.Usuario;
import com.challenge.challenge_backend.Models.Usuario.Role;

import com.challenge.challenge_backend.Repository.UsuarioRepository;
import com.challenge.challenge_backend.Security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación y autorización.
 * 
 * Maneja login, registro y gestión de tokens.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService implements UserDetailsService {
    
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    
    /**
     * Login - Público
     */
    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Intento de login para usuario: {}", request.getEmail());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
            
            Usuario usuario = (Usuario) authentication.getPrincipal();
            
            String token = jwtService.generateToken(usuario);
            Long expiresIn = jwtService.getExpirationTime();
            
            log.info("Login exitoso para usuario: {} con rol: {}", 
                    usuario.getEmail(), usuario.getRole());
            
            return new AuthResponseDTO(
                token,
                expiresIn,
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRole().name()
            );
            
        } catch (BadCredentialsException e) {
            log.warn("Credenciales inválidas para usuario: {}", request.getEmail());
            throw new UnauthorizedException("Email o contraseña incorrectos. Por favor, verifique sus credenciales.");
        } catch (Exception e) {
            log.error("Error durante el login: {}", e.getMessage());
            throw new UnauthorizedException("Error al autenticar. Por favor, intente nuevamente.");
        }
    }
    
    /**
     * Registro - Solo ADMIN puede registrar nuevos usuarios
     * CORREGIDO: Ahora acepta solo RegisterRequestDTO y el adminEmail
     */
    public AuthResponseDTO register(RegisterRequestDTO request, String adminEmail) {
        log.info("Admin {} registrando nuevo usuario: {}", adminEmail, request.getEmail());
        
        // Verificar que el usuario actual es ADMIN
        Usuario adminActual = usuarioRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new UnauthorizedException("Usuario administrador no encontrado"));
        
        if (adminActual.getRole() != Role.ADMIN) {
            log.warn("Usuario {} sin permisos intentó registrar nuevo usuario", adminEmail);
            throw new ForbiddenException(
                "Solo los administradores pueden registrar nuevos usuarios. " +
                "Su rol actual es: " + adminActual.getRole()
            );
        }
        
        // Verificar si el email ya existe
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            log.warn("Intento de registro con email duplicado: {}", request.getEmail());
            throw new BusinessException("El email ya está registrado: " + request.getEmail());
        }
        
        // Determinar el rol del nuevo usuario
        Role nuevoRol = Role.USER; // Por defecto USER
        if (request.getRole() != null) {
            try {
                nuevoRol = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Rol inválido. Use USER o ADMIN");
            }
        }
        
        // Solo ADMIN puede crear otro ADMIN
        if (nuevoRol == Role.ADMIN && adminActual.getRole() != Role.ADMIN) {
            throw new ForbiddenException(
                "Solo un administrador puede crear otro administrador."
            );
        }
        
        // Crear nuevo usuario
        Usuario nuevoUsuario = Usuario.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .role(nuevoRol)
                .build();
        
        nuevoUsuario = usuarioRepository.save(nuevoUsuario);
        
        log.info("Usuario {} registrado exitosamente con rol {} por admin {}", 
                nuevoUsuario.getEmail(), nuevoRol, adminEmail);
        
        // No generar token para el nuevo usuario, solo confirmar creación
        return AuthResponseDTO.builder()
                .token(null)  // No devolver token
                .email(nuevoUsuario.getEmail())
                .nombreCompleto(nuevoUsuario.getNombre() + " " + nuevoUsuario.getApellido())
                .role(nuevoUsuario.getRole().name())
                .issuedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Cambiar contraseña 
     */
    public void cambiarPassword(ChangePasswordRequestDTO request, String userEmail) {
        log.info("Cambio de contraseña solicitado para: {}", userEmail);
        
        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        
        // Verificar contraseña actual
        if (!passwordEncoder.matches(request.getOldPassword(), usuario.getPassword())) {
            log.warn("Contraseña actual incorrecta para usuario: {}", userEmail);
            throw new UnauthorizedException(
                "La contraseña actual es incorrecta. Por favor, verifique e intente nuevamente."
            );
        }
        
        // Validar que la nueva contraseña sea diferente
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(
                "La nueva contraseña debe ser diferente a la actual."
            );
        }
        
        // Actualizar contraseña
        usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usuarioRepository.save(usuario);
        
        log.info("Contraseña actualizada exitosamente para: {}", userEmail);
    }
    
    // ========================================
    // VALIDAR TOKEN
    // ========================================
    
    /**
     * Valida si un token JWT es válido.
     * 
     * @param token Token JWT a validar
     * @return true si el token es válido, false en caso contrario
     */
    public boolean validarToken(String token) {
        try {
            String email = jwtService.extractUsername(token);
            UserDetails userDetails = loadUserByUsername(email);
            return jwtService.isTokenValid(token, userDetails);
        } catch (Exception e) {
            log.error("Error validando token: {}", e.getMessage());
            return false;
        }
    }
    
    // ========================================
    // UserDetailsService Implementation
    // ========================================
    
    /**
     * Carga un usuario por su email (username).
     * Requerido por Spring Security.
     * 
     * @param username Email del usuario
     * @return UserDetails del usuario
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByEmailAndActivoTrue(username)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado o inactivo: {}", username);
                    return new UsernameNotFoundException("Usuario no encontrado: " + username);
                });
    }
    
    // ========================================
    // MÉTODOS AUXILIARES
    // ========================================
    
    /**
     * Obtiene el usuario actual desde el contexto de seguridad.
     * 
     * @param email Email del usuario autenticado
     * @return Usuario actual
     */
    public Usuario obtenerUsuarioActual(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
    
    /**
     * Obtener el usuario actual desde el contexto de seguridad
     */
    public String getUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();  // Retorna el email del usuario
        }
        return "Sistema";
    }


    /**
     * Verifica si un usuario tiene un rol específico.
     * 
     * @param email Email del usuario
     * @param role Rol a verificar
     * @return true si el usuario tiene el rol
     */
    public boolean usuarioTieneRol(String email, Role role) {
        Usuario usuario = obtenerUsuarioActual(email);
        return usuario.getRole() == role;
    }
    
    /**
     * Crea un usuario administrador inicial (para setup).
     * Solo usar en desarrollo o configuración inicial.
     */
    public void crearAdminInicial() {
        String adminEmail = "admin@challenge.com";
        
        if (!usuarioRepository.existsByEmail(adminEmail)) {
            Usuario admin = Usuario.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin123!"))
                    .nombre("Admin")
                    .apellido("Sistema")
                    .role(Role.ADMIN)
                    .build();
            
            usuarioRepository.save(admin);
            log.info("Usuario administrador inicial creado: {}", adminEmail);
        }
    }
}