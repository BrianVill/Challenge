package com.challenge.challenge_backend.Controller;

import com.challenge.challenge_backend.DTOs.Request.BatchClienteRequestDTO;
import com.challenge.challenge_backend.DTOs.Request.ClienteRequestDTO;
import com.challenge.challenge_backend.DTOs.Response.ApiResponseDTO;
import com.challenge.challenge_backend.DTOs.Response.BatchResponseDTO;
import com.challenge.challenge_backend.DTOs.Response.ClienteResponseDTO;
import com.challenge.challenge_backend.DTOs.Response.EstadisticasClienteDTO;
import com.challenge.challenge_backend.Exception.BusinessException;
import com.challenge.challenge_backend.Service.ClienteService;
import com.challenge.challenge_backend.Service.EmailService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Controller principal para gestión de clientes.
 * 
 * Implementando los 3 endpoints requeridos en el challenge:
 * 1. POST /creacliente - Crear nuevos clientes
 * 2. GET /kpideclientes - Obtener estadísticas (promedio y desviación estándar)
 * 3. GET /listclientes - Listar todos los clientes con fecha probable de
 * fallecimiento
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clientes", description = "API para gestión de clientes")
@CrossOrigin(origins = "*") // Permitir CORS para pruebas
public class ClienteController {

    private final ClienteService clienteService;
    private final EmailService emailService;

    /**
     * Crear cliente con auditoría de usuario y email opcional.
     */
    @PostMapping("/creacliente")
    @Operation(
        summary = "Crear nuevo cliente",
        description = "Registra un nuevo cliente. Requiere autenticación. Registra quién creó el cliente."
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponseDTO<ClienteResponseDTO>> crearCliente(
            @Valid @RequestBody ClienteRequestDTO request,
            @RequestParam(required = false) String emailDestino,
            Authentication authentication) {
        
        // Obtener el email del usuario actual
        String usuarioActual = authentication.getPrincipal().toString();
        if (authentication.getPrincipal() instanceof UserDetails) {
            usuarioActual = ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        
        log.info("POST /api/creacliente - Usuario {} creando cliente: {} {}", 
                usuarioActual, request.getNombre(), request.getApellido());
        
        ClienteResponseDTO cliente = clienteService.crearCliente(request, emailDestino);
        
        String mensaje = String.format(
            "Cliente creado exitosamente por el usuario: %s", 
            usuarioActual
        );
        
        ApiResponseDTO<ClienteResponseDTO> response = ApiResponseDTO.success(
            cliente, 
            mensaje
        );
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ========================================
    // 2. KPI DE CLIENTES (Endpoint requerido)
    // ========================================

    /**
     * Endpoint para obtener estadísticas de clientes.
     */
    @GetMapping("/kpideclientes")
    @Operation(
        summary = "Obtener KPIs de clientes",
        description = "Retorna estadísticas. Requiere autenticación."
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponseDTO<EstadisticasClienteDTO>> obtenerKpiClientes(
            @RequestParam(required = false) String emailDestino) {
        
        log.info("GET /api/kpideclientes - Obteniendo estadísticas");
        
        EstadisticasClienteDTO estadisticas = clienteService.calcularEstadisticas(emailDestino);
        
        ApiResponseDTO<EstadisticasClienteDTO> response = ApiResponseDTO.success(
            estadisticas,
            "Estadísticas calculadas exitosamente"
        );
        
        return ResponseEntity.ok(response);
    }

    // ========================================
    // 3. LISTAR CLIENTES (Endpoint requerido)
    // ========================================

    /**
     * Endpoint para listar todos los clientes.
     */
    @GetMapping("/listclientes")
    @Operation(
        summary = "Listar todos los clientes",
        description = "Retorna lista de clientes. Requiere autenticación."
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponseDTO<List<ClienteResponseDTO>>> listarClientes(
            @RequestParam(required = false) String emailDestino) {
        
        log.info("GET /api/listclientes - Listando todos los clientes");
        
        List<ClienteResponseDTO> clientes = clienteService.listarTodosLosClientes();
        
        if (emailDestino != null && !emailDestino.isEmpty()) {
            emailService.enviarListadoClientes(clientes, emailDestino);
        }
        
        String mensaje = String.format("Se encontraron %d clientes", clientes.size());
        
        ApiResponseDTO<List<ClienteResponseDTO>> response = ApiResponseDTO.success(
            clientes,
            mensaje
        );
        
        return ResponseEntity.ok(response);
    }

    // ========================================
    // ENDPOINTS ADICIONALES (Opcionales pero útiles)
    // ========================================

    /**
     * Listar clientes con paginación.
     * Útil cuando hay muchos clientes.
     */
    @GetMapping("/clientes")
    @Operation(summary = "Listar clientes con paginación", description = "Retorna una página de clientes con opciones de paginación y ordenamiento")
    public ResponseEntity<ApiResponseDTO<Page<ClienteResponseDTO>>> listarClientesPaginados(
            @Parameter(description = "Número de página (comienza en 0)") @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "fechaRegistro") String sortBy,

            @Parameter(description = "Dirección del orden: ASC o DESC") @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("GET /api/clientes - Página: {}, Tamaño: {}", page, size);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ClienteResponseDTO> clientesPage = clienteService.listarClientes(pageable);

        ApiResponseDTO<Page<ClienteResponseDTO>> response = ApiResponseDTO.success(
                clientesPage,
                "Clientes obtenidos exitosamente");

        return ResponseEntity.ok(response);
    }

    /**
     * Obtener un cliente por ID.
     */
    @GetMapping("/clientes/{id}")
    @Operation(summary = "Obtener cliente por ID", description = "Retorna los detalles de un cliente específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ApiResponseDTO<ClienteResponseDTO>> obtenerClientePorId(
            @Parameter(description = "ID del cliente") @PathVariable Long id) {

        log.info("GET /api/clientes/{} - Buscando cliente", id);

        ClienteResponseDTO cliente = clienteService.obtenerClientePorId(id);

        ApiResponseDTO<ClienteResponseDTO> response = ApiResponseDTO.success(
                cliente,
                "Cliente encontrado");

        return ResponseEntity.ok(response);
    }

    /**
     * Actualizar cliente con email opcional.
     */
    @PutMapping("/clientes/{id}")
    @Operation(
        summary = "Actualizar cliente",
        description = "Actualiza un cliente existente. Opción de enviar notificación por email."
    )
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponseDTO<ClienteResponseDTO>> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequestDTO request,
            @RequestParam(required = false) String emailDestino,
            Authentication authentication) {
        
        String usuarioActual = authentication.getPrincipal().toString();
        if (authentication.getPrincipal() instanceof UserDetails) {
            usuarioActual = ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        
        log.info("PUT /api/clientes/{} - Usuario {} actualizando cliente", id, usuarioActual);
        
        ClienteResponseDTO cliente = clienteService.actualizarCliente(id, request, emailDestino);
        
        String mensaje = String.format(
            "Cliente actualizado exitosamente por el usuario: %s",
            usuarioActual
        );
        
        ApiResponseDTO<ClienteResponseDTO> response = ApiResponseDTO.success(
            cliente,
            mensaje
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Eliminar cliente con email opcional.
     */
    @DeleteMapping("/clientes/{id}")
    @Operation(
        summary = "Eliminar cliente",
        description = "Elimina un cliente (soft delete). Solo ADMIN. Opción de enviar notificación por email."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> eliminarCliente(
            @PathVariable Long id,
            @RequestParam(required = false) String emailDestino,
            Authentication authentication) {
        
        String adminActual = authentication.getPrincipal().toString();
        if (authentication.getPrincipal() instanceof UserDetails) {
            adminActual = ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        
        log.info("DELETE /api/clientes/{} - Admin {} eliminando cliente", id, adminActual);
        
        clienteService.eliminarCliente(id, emailDestino);
        
        ApiResponseDTO<Void> response = ApiResponseDTO.successMessage(
            String.format("Cliente eliminado exitosamente por el administrador: %s", adminActual)
        );
        
        return ResponseEntity.ok(response);
    }

    // ========================================
    // CREACIÓN MASIVA DE CLIENTES
    // ========================================

    /**
     * Crear múltiples clientes con email opcional.
     */
    @PostMapping("/creaclientes/batch")
    @Operation(
        summary = "Crear múltiples clientes",
        description = "Crea varios clientes en una sola operación. Solo ADMIN. Opción de enviar resumen por email."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<BatchResponseDTO>> crearClientesMasivo(
            @Valid @RequestBody BatchClienteRequestDTO request,
            @RequestParam(required = false) String emailDestino,
            Authentication authentication) {
        
        String adminEmail = authentication.getPrincipal().toString();
        if (authentication.getPrincipal() instanceof UserDetails) {
            adminEmail = ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        
        log.info("POST /api/creaclientes/batch - Admin {} creando {} clientes", 
                adminEmail, request.getClientes().size());
        
        if (request.getClientes().size() > 100) {
            throw new BusinessException("El límite máximo es 100 clientes por batch");
        }
        
        BatchResponseDTO resultado = clienteService.crearClientesMasivo(
            request.getClientes(), 
            emailDestino
        );
        
        String mensaje = String.format(
            "Procesamiento completado por %s: %d exitosos, %d fallidos de %d total",
            adminEmail,
            resultado.getExitosos(), 
            resultado.getFallidos(), 
            resultado.getTotal()
        );
        
        ApiResponseDTO<BatchResponseDTO> response = ApiResponseDTO.success(
            resultado,
            mensaje
        );
        
        HttpStatus status = resultado.getFallidos() > 0 
            ? HttpStatus.MULTI_STATUS 
            : HttpStatus.CREATED;
        
        return new ResponseEntity<>(response, status);
    }

    /**
     * Modificación del endpoint individual para agregar seguridad opcional.
     * Cualquier usuario autenticado puede crear clientes individuales.
     */
    @PostMapping("/creacliente/v2")
    @Operation(summary = "Crear cliente (con autenticación)", description = "Crea un cliente individual. Requiere autenticación (USER o ADMIN).")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // USER o ADMIN pueden crear
    public ResponseEntity<ApiResponseDTO<ClienteResponseDTO>> crearClienteConAuth(
            @Valid @RequestBody ClienteRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) { // Obtener usuario actual

        log.info("POST /api/creacliente/v2 - Usuario {} creando cliente: {} {}",
                userDetails.getUsername(), request.getNombre(), request.getApellido());

        ClienteResponseDTO cliente = clienteService.crearCliente(request);

        ApiResponseDTO<ClienteResponseDTO> response = ApiResponseDTO.success(
                cliente,
                String.format("Cliente creado por usuario: %s", userDetails.getUsername()));

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Health check del servicio.
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Verifica que el servicio esté funcionando")
    public ResponseEntity<ApiResponseDTO<String>> healthCheck() {

        ApiResponseDTO<String> response = ApiResponseDTO.success(
                "Service is running",
                "Cliente API está funcionando correctamente");

        return ResponseEntity.ok(response);
    }

    
}
