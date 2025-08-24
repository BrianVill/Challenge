package com.challenge.challenge_backend.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Jackson ObjectMapper.
 * Necesario para serializar/deserializar JSON correctamente.
 */
@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Registrar módulo para manejar LocalDateTime y otras clases de Java 8 Time
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
