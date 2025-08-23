package com.challenge.challenge_backend.Config;

import com.challenge.challenge_backend.Security.JwtAuthenticationEntryPoint;
import com.challenge.challenge_backend.Security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad actualizada.
 * Todos los endpoints requieren autenticación excepto login.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            
            .authorizeHttpRequests(auth -> auth
                // Solo estos endpoints son públicos
                .requestMatchers(
                    "/api/auth/login",           // Login público
                    "/api/health",                // Health check público
                    "/swagger-ui/**",             // Documentación
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                
                // Register solo para ADMIN
                .requestMatchers("/api/auth/register").hasRole("ADMIN")
                
                // Cambio de contraseña para usuarios autenticados
                .requestMatchers("/api/auth/change-password").authenticated()
                
                // Todos los endpoints de clientes requieren autenticación
                .requestMatchers("/api/creacliente").authenticated()
                .requestMatchers("/api/kpideclientes").authenticated()
                .requestMatchers("/api/listclientes").authenticated()
                .requestMatchers("/api/clientes/**").authenticated()
                .requestMatchers("/api/creaclientes/batch").hasRole("ADMIN")
                
                // Cualquier otro request requiere autenticación
                .anyRequest().authenticated()
            )
            
            // Manejo de excepciones de autenticación
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}