package com.challenge.challenge_backend.Service;

import com.challenge.challenge_backend.DTOs.Request.LoginRequestDTO;
import com.challenge.challenge_backend.DTOs.Request.RegisterRequestDTO;
import com.challenge.challenge_backend.DTOs.Response.AuthResponseDTO;
import com.challenge.challenge_backend.Exception.BusinessException;
import com.challenge.challenge_backend.Exception.UnauthorizedException;
import com.challenge.challenge_backend.Models.Usuario;
import com.challenge.challenge_backend.Models.Usuario.Role;

import com.challenge.challenge_backend.Repository.UsuarioRepository;
import com.challenge.challenge_backend.Security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    
    // ========================================
    // LOGIN
    // ========================================
    
    /**
     * Autentica un usuario y genera un token JWT.
     * 
     * @param request DTO con credenciales de login
     * @return DTO con token JWT y datos del usuario
     * @throws UnauthorizedException si las credenciales son inválidas
     */
    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Intento de login para usuario: {}", request.getEmail());
        
        try {
            // 1. Autenticar las credenciales
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );
            
            // 2. Obtener el usuario autenticado
            Usuario usuario = (Usuario) authentication.getPrincipal();
            
            // 3. Generar token JWT
            String token = jwtService.generateToken(usuario);
            Long expiresIn = jwtService.getExpirationTime();
            
            log.info("Login exitoso para usuario: {}", usuario.getEmail());
            
            // 4. Construir respuesta
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
            throw new UnauthorizedException("Email o contraseña incorrectos");
        } catch (Exception e) {
            log.error("Error durante el login: {}", e.getMessage());
            throw new UnauthorizedException("Error al autenticar usuario");
        }
    }
    
    // ========================================
    // REGISTRO
    // ========================================
    
    /**
     * Registra un nuevo usuario en el sistema.
     * 
     * @param request DTO con datos del nuevo usuario
     * @return DTO con token JWT del usuario registrado
     * @throws BusinessException si el email ya está registrado
     */
    public AuthResponseDTO register(RegisterRequestDTO request) {
        log.info("Registro de nuevo usuario: {}", request.getEmail());
        
        // 1. Verificar si el email ya existe
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            log.warn("Intento de registro con email duplicado: {}", request.getEmail());
            throw new BusinessException("El email ya está registrado");
        }
        
        // 2. Crear nuevo usuario
        Usuario nuevoUsuario = Usuario.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .role(Role.USER)  // Por defecto, rol USER
                .build();
        
        // 3. Guardar en la base de datos
        nuevoUsuario = usuarioRepository.save(nuevoUsuario);
        log.info("Usuario registrado exitosamente: {}", nuevoUsuario.getEmail());
        
        // 4. Generar token para auto-login
        String token = jwtService.generateToken(nuevoUsuario);
        Long expiresIn = jwtService.getExpirationTime();
        
        // 5. Retornar respuesta con token
        return new AuthResponseDTO(
            token,
            expiresIn,
            nuevoUsuario.getEmail(),
            nuevoUsuario.getNombre(),
            nuevoUsuario.getApellido(),
            nuevoUsuario.getRole().name()
        );
    }
    
    // ========================================
    // CAMBIAR CONTRASEÑA
    // ========================================
    
    /**
     * Cambia la contraseña de un usuario.
     * 
     * @param email Email del usuario
     * @param oldPassword Contraseña actual
     * @param newPassword Nueva contraseña
     * @throws UnauthorizedException si la contraseña actual es incorrecta
     */
    public void cambiarPassword(String email, String oldPassword, String newPassword) {
        log.info("Cambio de contraseña solicitado para: {}", email);
        
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        
        // Verificar contraseña actual
        if (!passwordEncoder.matches(oldPassword, usuario.getPassword())) {
            log.warn("Contraseña actual incorrecta para usuario: {}", email);
            throw new UnauthorizedException("La contraseña actual es incorrecta");
        }
        
        // Actualizar contraseña
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
        
        log.info("Contraseña actualizada exitosamente para: {}", email);
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