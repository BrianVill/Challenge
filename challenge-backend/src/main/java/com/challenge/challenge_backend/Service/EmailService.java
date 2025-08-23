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

    /**
     * Enviar notificación personalizada a un email específico.
     */
    @Async
    public CompletableFuture<Boolean> enviarNotificacionPersonalizada(
            String emailDestino, 
            String asunto, 
            String mensaje) {
        
        log.info("Enviando notificación personalizada a: {}", emailDestino);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(emailDestino);
            message.setSubject(asunto);
            message.setText(mensaje);
            
            mailSender.send(message);
            
            log.info("Email enviado exitosamente a: {}", emailDestino);
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", emailDestino, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Enviar estadísticas a un email específico.
     */
    @Async
    public CompletableFuture<Boolean> enviarNotificacionEstadisticasCalculadas(
            EstadisticasClienteDTO estadisticas, 
            String emailDestino) {
        
        log.info("Enviando estadísticas a: {}", emailDestino);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(emailDestino);
            helper.setSubject("📊 Estadísticas de Clientes - " + LocalDateTime.now().format(formatter));
            
            String contenidoHtml = construirEmailEstadisticas(estadisticas);
            helper.setText(contenidoHtml, true);
            
            mailSender.send(message);
            
            log.info("Estadísticas enviadas a: {}", emailDestino);
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            log.error("Error enviando estadísticas a {}: {}", emailDestino, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Enviar listado de clientes.
     */
    @Async
    public void enviarListadoClientes(List<ClienteResponseDTO> clientes, String emailDestino) {
        log.info("Enviando listado de {} clientes a: {}", clientes.size(), emailDestino);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(emailDestino);
            helper.setSubject("📋 Listado de Clientes - Total: " + clientes.size());
            
            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family: Arial, sans-serif;'>");
            html.append("<h2>Listado de Clientes Activos</h2>");
            html.append("<p>Total de clientes: ").append(clientes.size()).append("</p>");
            html.append("<table style='border-collapse: collapse; width: 100%;'>");
            html.append("<thead style='background-color: #f2f2f2;'>");
            html.append("<tr>");
            html.append("<th style='border: 1px solid #ddd; padding: 8px;'>ID</th>");
            html.append("<th style='border: 1px solid #ddd; padding: 8px;'>Nombre</th>");
            html.append("<th style='border: 1px solid #ddd; padding: 8px;'>Apellido</th>");
            html.append("<th style='border: 1px solid #ddd; padding: 8px;'>Edad</th>");
            html.append("<th style='border: 1px solid #ddd; padding: 8px;'>Fecha Nacimiento</th>");
            html.append("<th style='border: 1px solid #ddd; padding: 8px;'>Años Restantes</th>");
            html.append("</tr></thead><tbody>");
            
            for (ClienteResponseDTO cliente : clientes) {
                html.append("<tr>");
                html.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(cliente.getId()).append("</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(cliente.getNombre()).append("</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(cliente.getApellido()).append("</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(cliente.getEdad()).append("</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(cliente.getFechaNacimiento()).append("</td>");
                html.append("<td style='border: 1px solid #ddd; padding: 8px;'>").append(cliente.getAñosRestantes()).append("</td>");
                html.append("</tr>");
            }
            
            html.append("</tbody></table>");
            html.append("<p style='margin-top: 20px; color: #666;'>Generado: ").append(LocalDateTime.now().format(formatter)).append("</p>");
            html.append("</body></html>");
            
            helper.setText(html.toString(), true);
            mailSender.send(message);
            
            log.info("Listado enviado exitosamente a: {}", emailDestino);
            
        } catch (Exception e) {
            log.error("Error enviando listado a {}: {}", emailDestino, e.getMessage());
        }
    }
    
    /**
     * Notificación de actualización de cliente.
     */
    @Async
    public void enviarNotificacionActualizacion(
            ClienteResponseDTO cliente, 
            String usuarioActualizador, 
            String emailDestino) {
        
        log.info("Enviando notificación de actualización a: {}", emailDestino);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(emailDestino);
            message.setSubject("✏️ Cliente Actualizado - " + cliente.getNombre() + " " + cliente.getApellido());
            
            String contenido = String.format("""
                Cliente Actualizado
                ==================
                
                ID: %d
                Nombre: %s %s
                Edad: %d años
                Fecha de Nacimiento: %s
                
                Actualizado por: %s
                Fecha de actualización: %s
                
                Este es un mensaje automático del sistema.
                """,
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getEdad(),
                cliente.getFechaNacimiento(),
                usuarioActualizador,
                LocalDateTime.now().format(formatter)
            );
            
            message.setText(contenido);
            mailSender.send(message);
            
            log.info("Notificación de actualización enviada a: {}", emailDestino);
            
        } catch (Exception e) {
            log.error("Error enviando notificación de actualización: {}", e.getMessage());
        }
    }
    
    /**
     * Notificación de eliminación de cliente.
     */
    @Async
    public void enviarNotificacionEliminacion(
            Long clienteId, 
            String adminEliminador, 
            String emailDestino) {
        
        log.info("Enviando notificación de eliminación a: {}", emailDestino);
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(emailDestino);
            message.setSubject("🗑️ Cliente Eliminado - ID: " + clienteId);
            
            String contenido = String.format("""
                Cliente Eliminado del Sistema
                =============================
                
                ID del Cliente: %d
                
                Eliminado por: %s (ADMINISTRADOR)
                Fecha de eliminación: %s
                
                Nota: Esta es una eliminación lógica (soft delete).
                El registro permanece en la base de datos pero marcado como inactivo.
                
                Este es un mensaje automático del sistema.
                """,
                clienteId,
                adminEliminador,
                LocalDateTime.now().format(formatter)
            );
            
            message.setText(contenido);
            mailSender.send(message);
            
            log.info("Notificación de eliminación enviada a: {}", emailDestino);
            
        } catch (Exception e) {
            log.error("Error enviando notificación de eliminación: {}", e.getMessage());
        }
    }
}
