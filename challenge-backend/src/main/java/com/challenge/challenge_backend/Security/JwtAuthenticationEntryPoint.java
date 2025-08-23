package com.challenge.challenge_backend.Security;

import com.challenge.challenge_backend.DTOs.Response.ApiResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Maneja errores cuando un usuario no autenticado intenta acceder a recursos protegidos.
 * Devuelve un mensaje claro indicando que debe hacer login primero.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        ApiResponseDTO<Void> errorResponse = ApiResponseDTO.<Void>builder()
            .success(false)
            .message("Debe iniciar sesión primero. Por favor, use el endpoint /api/auth/login para obtener un token de autenticación.")
            .errorCode("NOT_AUTHENTICATED")
            .path(request.getRequestURI())
            .timestamp(LocalDateTime.now())
            .build();
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
