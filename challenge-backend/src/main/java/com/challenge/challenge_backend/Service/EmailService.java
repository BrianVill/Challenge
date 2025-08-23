package com.challenge.challenge_backend.Service;

import com.challenge.challenge_backend.DTOs.Response.ClienteResponseDTO;
import com.challenge.challenge_backend.DTOs.Response.EstadisticasClienteDTO;
import com.challenge.challenge_backend.Models.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de notificaciones por email.
 * 
 * Todos los métodos son ASÍNCRONOS para no bloquear el hilo principal.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.mail.admin-email:admin@challenge.com}")
    private String adminEmail;
    
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // ========================================
    // NOTIFICACIÓN DE CLIENTE CREADO
    // ========================================
    
    /**
     * Envía email asíncrono cuando se crea un nuevo cliente.
     * No bloquea el hilo principal.
     */
    @Async
    public CompletableFuture<Boolean> enviarNotificacionClienteCreado(ClienteResponseDTO cliente) {
        log.info("Iniciando envío asíncrono de email para cliente creado: {}", cliente.getId());
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("✅ Nuevo Cliente Registrado - ID: " + cliente.getId());
            
            String contenidoHtml = construirEmailClienteCreado(cliente);
            helper.setText(contenidoHtml, true);
            
            mailSender.send(message);
            
            log.info("Email enviado exitosamente para cliente: {}", cliente.getId());
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("Error enviando email para cliente creado: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Construye el HTML del email de cliente creado.
     */
    private String construirEmailClienteCreado(ClienteResponseDTO cliente) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="background-color: #f0f8ff; padding: 20px; border-radius: 10px;">
                    <h2 style="color: #2c3e50;">🎉 Nuevo Cliente Registrado</h2>
                    
                    <h3>Método Completado: <code>crearCliente()</code></h3>
                    
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr style="background-color: #e8f4f8;">
                            <td style="padding: 10px; border: 1px solid #ddd;"><strong>ID:</strong></td>
                            <td style="padding: 10px; border: 1px solid #ddd;">%d</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; border: 1px solid #ddd;"><strong>Nombre:</strong></td>
                            <td style="padding: 10px; border: 1px solid #ddd;">%s %s</td>
                        </tr>
                        <tr style="background-color: #e8f4f8;">
                            <td style="padding: 10px; border: 1px solid #ddd;"><strong>Edad:</strong></td>
                            <td style="padding: 10px; border: 1px solid #ddd;">%d años</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; border: 1px solid #ddd;"><strong>Fecha Nacimiento:</strong></td>
                            <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr style="background-color: #e8f4f8;">
                            <td style="padding: 10px; border: 1px solid #ddd;"><strong>Fecha Probable Fallecimiento:</strong></td>
                            <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 10px; border: 1px solid #ddd;"><strong>Años Restantes:</strong></td>
                            <td style="padding: 10px; border: 1px solid #ddd;">%d años</td>
                        </tr>
                        <tr style="background-color: #e8f4f8;">
                            <td style="padding: 10px; border: 1px solid #ddd;"><strong>Fecha de Registro:</strong></td>
                            <td style="padding: 10px; border: 1px solid #ddd;">%s</td>
                        </tr>
                    </table>
                    
                    <p style="color: #7f8c8d; margin-top: 20px;">
                        <small>Este es un mensaje automático generado de forma asíncrona.</small>
                    </p>
                </div>
            </body>
            </html>
            """,
            cliente.getId(),
            cliente.getNombre(),
            cliente.getApellido(),
            cliente.getEdad(),
            cliente.getFechaNacimiento(),
            cliente.getFechaProbableFallecimiento(),
            cliente.getAñosRestantes(),
            LocalDateTime.now().format(formatter)
        );
    }
    
    // ========================================
    // NOTIFICACIÓN DE ESTADÍSTICAS CALCULADAS
    // ========================================
    
    @Async
    public CompletableFuture<Boolean> enviarNotificacionEstadisticasCalculadas(EstadisticasClienteDTO estadisticas) {
        log.info("Enviando email asíncrono de estadísticas calculadas");
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("📊 Estadísticas de Clientes Calculadas");
            
            String contenidoHtml = construirEmailEstadisticas(estadisticas);
            helper.setText(contenidoHtml, true);
            
            mailSender.send(message);
            
            log.info("Email de estadísticas enviado exitosamente");
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("Error enviando email de estadísticas: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
    
    private String construirEmailEstadisticas(EstadisticasClienteDTO stats) {
        StringBuilder distribucion = new StringBuilder();
        if (stats.getDistribucionPorRangoEdad() != null) {
            stats.getDistribucionPorRangoEdad().forEach((rango, cantidad) -> 
                distribucion.append(String.format("<li>%s: %d clientes</li>", rango, cantidad))
            );
        }
        
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <div style="background-color: #f5f5f5; padding: 20px; border-radius: 10px;">
                    <h2 style="color: #34495e;">📊 Estadísticas Calculadas</h2>
                    
                    <h3>Método Completado: <code>calcularEstadisticas()</code></h3>
                    
                    <div style="background-color: white; padding: 15px; border-radius: 5px; margin: 10px 0;">
                        <h4>Resumen General:</h4>
                        <ul>
                            <li><strong>Total de Clientes:</strong> %d</li>
                            <li><strong>Promedio de Edad:</strong> %.2f años</li>
                            <li><strong>Desviación Estándar:</strong> %.2f</li>
                            <li><strong>Edad Mínima:</strong> %d años</li>
                            <li><strong>Edad Máxima:</strong> %d años</li>
                        </ul>
                    </div>
                    
                    <div style="background-color: white; padding: 15px; border-radius: 5px; margin: 10px 0;">
                        <h4>Distribución por Rango de Edad:</h4>
                        <ul>%s</ul>
                    </div>
                    
                    <p style="color: #95a5a6; margin-top: 20px;">
                        <small>Calculado: %s</small>
                    </p>
                </div>
            </body>
            </html>
            """,
            stats.getTotalClientes(),
            stats.getPromedioEdad(),
            stats.getDesviacionEstandar(),
            stats.getEdadMinima() != null ? stats.getEdadMinima() : 0,
            stats.getEdadMaxima() != null ? stats.getEdadMaxima() : 0,
            distribucion.toString(),
            LocalDateTime.now().format(formatter)
        );
    }
    
    // ========================================
    // NOTIFICACIÓN DE MÚLTIPLES USUARIOS CREADOS
    // ========================================
    
    @Async
    public CompletableFuture<Boolean> enviarResumenUsuariosCreados(List<Usuario> usuarios) {
        log.info("Enviando resumen asíncrono de {} usuarios creados", usuarios.size());
        
        try {
            long totalUsers = usuarios.stream()
                    .filter(u -> u.getRole() == Usuario.Role.USER)
                    .count();
            
            long totalAdmins = usuarios.stream()
                    .filter(u -> u.getRole() == Usuario.Role.ADMIN)
                    .count();
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(adminEmail);
            message.setSubject("👥 Resumen de Usuarios Creados");
            
            String contenido = String.format("""
                PROCESO COMPLETADO: Creación Masiva de Usuarios
                ================================================
                
                Método: crearUsuariosMasivos()
                Fecha: %s
                
                RESUMEN:
                --------
                • Total de usuarios creados: %d
                • Usuarios con rol USER: %d
                • Usuarios con rol ADMIN: %d
                
                DETALLES:
                ---------
                %s
                
                Este proceso se ejecutó de forma asíncrona.
                """,
                LocalDateTime.now().format(formatter),
                usuarios.size(),
                totalUsers,
                totalAdmins,
                construirDetalleUsuarios(usuarios)
            );
            
            message.setText(contenido);
            mailSender.send(message);
            
            log.info("Resumen de usuarios enviado exitosamente");
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("Error enviando resumen de usuarios: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
    
    private String construirDetalleUsuarios(List<Usuario> usuarios) {
        StringBuilder detalle = new StringBuilder();
        usuarios.forEach(u -> {
            detalle.append(String.format("• %s %s (%s) - Rol: %s\n", 
                u.getNombre(), 
                u.getApellido(), 
                u.getEmail(), 
                u.getRole()));
        });
        return detalle.toString();
    }
    
    // ========================================
    // NOTIFICACIÓN DE ERROR
    // ========================================
    
    @Async
    public void enviarNotificacionError(String metodo, String error) {
        log.error("Enviando notificación de error para método: {}", metodo);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(adminEmail);
            message.setSubject("⚠️ Error en Proceso: " + metodo);
            message.setText(String.format("""
                Se ha producido un error en el sistema.
                
                Método: %s
                Fecha: %s
                Error: %s
                
                Por favor, revise los logs para más detalles.
                """,
                metodo,
                LocalDateTime.now().format(formatter),
                error
            ));
            
            mailSender.send(message);
            
        } catch (Exception e) {
            log.error("No se pudo enviar email de error: {}", e.getMessage());
        }
    }
}
