package com.challenge.challenge_backend.Config;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Inicializador de datos.
 * Crea el usuario administrador inicial si no existe.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {
    
    private final com.challenge.challenge_backend.Service.AuthService authService;
    
    @Bean
    CommandLineRunner init() {
        return args -> {
            log.info("Verificando usuario administrador inicial...");
            authService.crearAdminInicial();
            log.info("========================================");
            log.info("CREDENCIALES DE ADMINISTRADOR INICIAL:");
            log.info("Email: admin@challenge.com");
            log.info("Password: Admin123!");
            log.info("IMPORTANTE: Cambie esta contraseña inmediatamente");
            log.info("========================================");
        };
    }
}
