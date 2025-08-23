package com.challenge.challenge_backend.Controller;

import com.challenge.challenge_backend.DTOs.Request.ChangePasswordRequestDTO;
import com.challenge.challenge_backend.DTOs.Request.LoginRequestDTO;
import com.challenge.challenge_backend.DTOs.Request.RegisterRequestDTO;
import com.challenge.challenge_backend.DTOs.Response.ApiResponseDTO;
import com.challenge.challenge_backend.DTOs.Response.AuthResponseDTO;
import com.challenge.challenge_backend.Service.AuthService;
import com.google.api.Authentication;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para autenticación y autorización.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints para autenticación y registro")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Endpoint para login de usuarios.
     * No requiere autenticación.
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login de usuario",
        description = "Autentica un usuario y retorna un token JWT"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login exitoso",
            content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciales inválidas",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de login inválidos",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))
        )
    })
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {
        
        log.info("POST /api/auth/login - Intento de login para: {}", request.getEmail());
        
        try {
            AuthResponseDTO authResponse = authService.login(request);
            
            ApiResponseDTO<AuthResponseDTO> response = ApiResponseDTO.success(
                authResponse,
                "Login exitoso"
            );
            
            log.info("Login exitoso para usuario: {}", request.getEmail());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error en login para {}: {}", request.getEmail(), e.getMessage());
            throw e; // GlobalExceptionHandler lo manejará
        }
    }
    
    @PostMapping("/register")
    @Operation(
        summary = "Registro de nuevo usuario",
        description = "Solo ADMIN puede registrar nuevos usuarios"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request,
            Authentication authentication) {
        
        // Obtener el email del admin actual
        String adminEmail = authentication.getPrincipal().toString();
        if (authentication.getPrincipal() instanceof UserDetails) {
            adminEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        
        log.info("POST /api/auth/register - Admin {} registrando usuario: {}", 
                adminEmail, request.getEmail());
        
        AuthResponseDTO authResponse = authService.register(request, adminEmail);
        
        String mensaje = String.format(
            "Usuario %s registrado exitosamente con rol %s por el administrador %s",
            request.getEmail(),
            request.getRole() != null ? request.getRole() : "USER",
            adminEmail
        );
        
        ApiResponseDTO<AuthResponseDTO> response = ApiResponseDTO.success(
            authResponse,
            mensaje
        );
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    public ResponseEntity<ApiResponseDTO<AuthResponseDTO>> register(
            @Valid @RequestBody RegisterRequestDTO request) {
        
        log.info("POST /api/auth/register - Registro de nuevo usuario: {}", request.getEmail());
        
        try {
            AuthResponseDTO authResponse = authService.register(request);
            
            ApiResponseDTO<AuthResponseDTO> response = ApiResponseDTO.success(
                authResponse,
                "Usuario registrado exitosamente"
            );
            
            log.info("Registro exitoso para: {}", request.getEmail());
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (Exception e) {
            log.error("Error registrando usuario {}: {}", request.getEmail(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Endpoint para validar si un token es válido.
     * Útil para verificar sesiones.
     */
    @GetMapping("/validate")
    @Operation(
        summary = "Validar token",
        description = "Verifica si un token JWT es válido"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token válido"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token inválido o expirado"
        )
    })
    public ResponseEntity<ApiResponseDTO<Boolean>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        log.debug("GET /api/auth/validate - Validando token");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponseDTO<Boolean> response = ApiResponseDTO.success(
                false,
                "Token no proporcionado o formato inválido"
            );
            return ResponseEntity.ok(response);
        }
        
        String token = authHeader.substring(7);
        boolean isValid = authService.validarToken(token);
        
        ApiResponseDTO<Boolean> response = ApiResponseDTO.success(
            isValid,
            isValid ? "Token válido" : "Token inválido"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint de prueba para verificar que auth funciona.
     * No requiere autenticación.
     */
    @GetMapping("/test")
    @Operation(
        summary = "Test de autenticación",
        description = "Endpoint de prueba que no requiere autenticación"
    )
    public ResponseEntity<ApiResponseDTO<String>> test() {
        
        ApiResponseDTO<String> response = ApiResponseDTO.success(
            "Auth service is working",
            "El servicio de autenticación está funcionando"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Endpoint protegido de ejemplo.
     * Requiere token válido (se configura en SecurityConfig).
     */
    @GetMapping("/protected")
    @Operation(
        summary = "Endpoint protegido",
        description = "Ejemplo de endpoint que requiere autenticación"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Acceso autorizado"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "No autorizado - Token requerido"
        )
    })
    public ResponseEntity<ApiResponseDTO<String>> protectedEndpoint(
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("GET /api/auth/protected - Acceso a endpoint protegido");
        
        ApiResponseDTO<String> response = ApiResponseDTO.success(
            "Has accedido a un endpoint protegido",
            "Autorización exitosa"
        );
        
        return ResponseEntity.ok(response);
    }

   /**
     * Cambiar contraseña - Usuario autenticado.
     */
    @PostMapping("/change-password")
    @Operation(
        summary = "Cambiar contraseña",
        description = "Permite cambiar la contraseña del usuario autenticado"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponseDTO<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request,
            Authentication authentication) {
        
        // Obtener el email del usuario actual
        String userEmail = authentication.getPrincipal().toString();
        if (authentication.getPrincipal() instanceof UserDetails) {
            userEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        
        log.info("POST /api/auth/change-password - Usuario {} cambiando contraseña", userEmail);
        
        authService.cambiarPassword(request, userEmail);
        
        ApiResponseDTO<Void> response = ApiResponseDTO.successMessage(
            "Contraseña actualizada exitosamente. Por favor, use su nueva contraseña en el próximo login."
        );
        
        return ResponseEntity.ok(response);
    }
}
