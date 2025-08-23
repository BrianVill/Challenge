package com.challenge.challenge_backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración para habilitar métodos asíncronos.
 * 
 * @EnableAsync permite usar @Async en los métodos
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Configura el pool de threads para tareas asíncronas.
     * Define cuántos procesos pueden ejecutarse en paralelo.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Configuración del pool de threads
        executor.setCorePoolSize(2);      // Threads mínimos
        executor.setMaxPoolSize(5);       // Threads máximos
        executor.setQueueCapacity(100);   // Cola de espera
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        
        return executor;
    }
}
