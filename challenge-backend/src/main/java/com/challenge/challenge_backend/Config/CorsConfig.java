package com.challenge.challenge_backend.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración CORS para permitir llamadas desde frontend.
 * IMPORTANTE: Ajustar según el ambiente (desarrollo/producción)
 */
@Configuration
public class CorsConfig {
    
    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        
        // En desarrollo permite todo, en producción especifica dominios
        if ("*".equals(allowedOrigins)) {
            corsConfiguration.setAllowedOrigins(Arrays.asList("*"));
        } else {
            // Para producción, especifica los dominios permitidos
            corsConfiguration.setAllowedOrigins(Arrays.asList(
                "https://tu-dominio-frontend.com",
                "https://www.tu-dominio-frontend.com",
                "http://tu-alb-frontend.us-east-1.elb.amazonaws.com" // Si tienes ALB
            ));
        }
        
        // Métodos HTTP permitidos
        corsConfiguration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Headers permitidos
        corsConfiguration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Cache-Control"
        ));
        
        // Permitir credenciales (cookies, authorization headers)
        corsConfiguration.setAllowCredentials(true);
        
        // Tiempo de cache para preflight requests
        corsConfiguration.setMaxAge(3600L);
        
        // Headers expuestos al cliente
        corsConfiguration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Disposition"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        
        return new CorsFilter(source);
    }
}