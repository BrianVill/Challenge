package com.challenge.challenge_backend.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que intercepta todas las peticiones HTTP para validar el token JWT.
 * 
 * Se ejecuta antes de cada request para:
 * 1. Extraer el token del header Authorization
 * 2. Validar el token
 * 3. Establecer la autenticación en el contexto de Spring Security
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;  // Tu servicio JWT existente
    private final UserDetailsService userDetailsService;
    
    /**
     * Método principal del filtro que se ejecuta en cada request.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // 1. Extraer el token del header
            String token = extractTokenFromRequest(request);
            
            // 2. Si no hay token, continuar sin autenticación
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // 3. Extraer el username del token
            String username = jwtService.extractUsername(token);
            
            // 4. Si el username es válido y no hay autenticación actual
            if (StringUtils.hasText(username) && 
                SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 5. Cargar los detalles del usuario
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // 6. Validar el token
                if (jwtService.isTokenValid(token, userDetails)) {
                    
                    // 7. Crear el objeto de autenticación
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );
                    
                    // 8. Establecer detalles adicionales
                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    
                    // 9. Establecer la autenticación en el contexto
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("Usuario autenticado: {}", username);
                } else {
                    log.debug("Token inválido para usuario: {}", username);
                }
            }
            
        } catch (Exception e) {
            log.error("Error procesando autenticación JWT: {}", e.getMessage());
            // No lanzar excepción, solo continuar sin autenticación
        }
        
        // 10. Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extrae el token JWT del header Authorization.
     * 
     * @param request HTTP request
     * @return Token JWT sin el prefijo "Bearer " o null si no existe
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Obtener el header Authorization
        String bearerToken = request.getHeader("Authorization");
        
        // Verificar que existe y tiene el formato correcto
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Retornar el token sin el prefijo "Bearer "
            return bearerToken.substring(7);
        }
        
        return null;
    }
    
    /**
     * Determina si este filtro debe ejecutarse para la request actual.
     * 
     * @return true si el filtro debe ejecutarse
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Puedes excluir ciertas rutas del filtro si lo deseas
        String path = request.getRequestURI();
        
        // Ejemplo: No filtrar rutas públicas
        return path.startsWith("/api/auth/") || 
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }
}
