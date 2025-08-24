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
        
        final String requestURI = request.getRequestURI();
        log.debug("Procesando request a: {}", requestURI);
        
        try {
            // Extraer el token del header
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                log.debug("Token encontrado, validando...");
                
                // Extraer el username del token
                String username = jwtService.extractUsername(token);
                
                // Si el username es válido y no hay autenticación actual
                if (StringUtils.hasText(username) && 
                    SecurityContextHolder.getContext().getAuthentication() == null) {
                    
                    log.debug("Cargando usuario: {}", username);
                    
                    // Cargar los detalles del usuario
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // Validar el token
                    if (jwtService.isTokenValid(token, userDetails)) {
                        
                        // Crear el objeto de autenticación
                        UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                            );
                        
                        // Establecer detalles adicionales
                        authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        
                        // Establecer la autenticación en el contexto
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        log.debug("Usuario autenticado exitosamente: {} con roles: {}", 
                                username, userDetails.getAuthorities());
                    } else {
                        log.warn("Token inválido para usuario: {}", username);
                    }
                }
            } else {
                log.debug("No se encontró token en el request");
            }
            
        } catch (Exception e) {
            log.error("Error procesando autenticación JWT: {}", e.getMessage());
            // No lanzar excepción, solo continuar sin autenticación
        }
        
        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extrae el token JWT del header Authorization.
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("Token extraído exitosamente");
            return token;
        }
        
        return null;
    }
    
    /**
     * Procesar todos los requests
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // Retornar false para procesar TODOS los requests
        return false;
    }
}
