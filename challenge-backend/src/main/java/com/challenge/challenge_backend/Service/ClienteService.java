package com.challenge.challenge_backend.Service;

import com.challenge.challenge_backend.DTOs.Request.ClienteRequestDTO;
import com.challenge.challenge_backend.DTOs.Response.BatchResponseDTO;
import com.challenge.challenge_backend.DTOs.Response.ClienteResponseDTO;
import com.challenge.challenge_backend.DTOs.Response.ErrorDetalleDTO;
import com.challenge.challenge_backend.DTOs.Response.EstadisticasClienteDTO;
import com.challenge.challenge_backend.Exception.BusinessException;
import com.challenge.challenge_backend.Exception.ResourceNotFoundException;
import com.challenge.challenge_backend.Repository.ClienteRepository;
import com.challenge.challenge_backend.Models.Cliente;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio que contiene la lógica de negocio para la gestión de clientes.
 * 
 * Implementa las operaciones CRUD y cálculos estadísticos requeridos.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j // Para logging
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EmailService emailService;
    private final AuthService authService;

    @Value("${app.cliente.esperanza-vida:75}")
    private int esperanzaVida; // Configurable desde application.properties

    // ========================================
    // CREAR CLIENTE
    // ========================================

   /**
     * Crea un nuevo cliente (sin el parámetro emailDestino para mantener compatibilidad).
     */
    @CacheEvict(value = {"estadisticas", "clientes"}, allEntries = true)
    public ClienteResponseDTO crearCliente(ClienteRequestDTO request) {
        // Obtener el usuario actual del contexto de seguridad
        String usuarioCreador = authService.getUsuarioActual();
        
        log.info("Usuario {} creando cliente: {} {}", 
                usuarioCreador, request.getNombre(), request.getApellido());
        
        // Validaciones
        validarCoherenciaEdadFechaNacimiento(request.getEdad(), request.getFechaNacimiento());
        
        boolean clienteExiste = clienteRepository.existsByNombreAndApellidoAndFechaNacimiento(
            request.getNombre(),
            request.getApellido(),
            request.getFechaNacimiento()
        );
        
        if (clienteExiste) {
            log.warn("Usuario {} intentó crear cliente duplicado: {} {}", 
                    usuarioCreador, request.getNombre(), request.getApellido());
            throw new BusinessException("Ya existe un cliente con los mismos datos");
        }
        
        // Crear la entidad Cliente
        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .edad(request.getEdad())
                .fechaNacimiento(request.getFechaNacimiento())
                .creadoPor(usuarioCreador)  // Registrar quién lo creó
                .build();
        
        cliente = clienteRepository.save(cliente);
        
        log.info("Cliente creado exitosamente con ID: {} por usuario: {}", 
                cliente.getId(), usuarioCreador);
        
        ClienteResponseDTO response = convertirAResponseDTO(cliente);
        
        // Si el emailService existe, enviar notificación al admin por defecto
        if (emailService != null) {
            CompletableFuture<Boolean> emailFuture = emailService.enviarNotificacionClienteCreado(response);
            
            emailFuture.thenAccept(enviado -> {
                if (enviado) {
                    log.info("Email de notificación enviado para cliente ID: {}", response.getId());
                } else {
                    log.warn("No se pudo enviar email para cliente ID: {}", response.getId());
                }
            });
        }
        
        return response;
    }

    /**
     * Sobrecarga del método para incluir email destino opcional.
     */
    @CacheEvict(value = {"estadisticas", "clientes"}, allEntries = true)
    public ClienteResponseDTO crearCliente(ClienteRequestDTO request, String emailDestino) {
        // Crear el cliente usando el método original
        ClienteResponseDTO response = crearCliente(request);
        
        // Si se especificó un email destino adicional, enviar notificación
        if (emailDestino != null && !emailDestino.isEmpty() && emailService != null) {
            String usuarioCreador = authService.getUsuarioActual();
            enviarNotificacionCreacion(response, usuarioCreador, emailDestino);
        }
        
        return response;
    }

    /**
     * Calcula estadísticas (sin emailDestino para compatibilidad).
     */
    @Cacheable(value = "estadisticas", key = "'general'")
    public EstadisticasClienteDTO calcularEstadisticas() {
        log.info("Calculando estadísticas de clientes");
        
        List<Cliente> clientesActivos = clienteRepository.findByActivoTrue();
        
        if (clientesActivos.isEmpty()) {
            return EstadisticasClienteDTO.builder()
                    .totalClientes(0L)
                    .promedioEdad(0.0)
                    .desviacionEstandar(0.0)
                    .fechaCalculo(LocalDateTime.now())  // Siempre establecer fecha
                    .mensaje("No hay clientes registrados en el sistema")
                    .build();
        }
        
        List<Integer> edades = clientesActivos.stream()
                .map(Cliente::getEdad)
                .collect(Collectors.toList());
        
        double promedio = calcularPromedio(edades);
        double desviacionEstandar = calcularDesviacionEstandar(edades, promedio);
        
        EstadisticasClienteDTO estadisticas = EstadisticasClienteDTO.builder()
                .totalClientes((long) clientesActivos.size())
                .promedioEdad(promedio)
                .desviacionEstandar(desviacionEstandar)
                .edadMinima(Collections.min(edades))
                .edadMaxima(Collections.max(edades))
                .medianaEdad(calcularMediana(edades))
                .distribucionPorRangoEdad(calcularDistribucionPorRangoEdad(edades))
                .fechaCalculo(LocalDateTime.now())  // Siempre establecer fecha
                .mensaje(String.format("Estadísticas calculadas para %d clientes activos", 
                        clientesActivos.size()))
                .build();
        
        return estadisticas;
    }
    
    /**
     * Sobrecarga del método para incluir email destino.
     */
    public EstadisticasClienteDTO calcularEstadisticas(String emailDestino) {
        EstadisticasClienteDTO estadisticas = calcularEstadisticas();
        
        // Enviar email si se especificó
        if (emailDestino != null && !emailDestino.isEmpty() && emailService != null) {
            emailService.enviarNotificacionEstadisticasCalculadas(estadisticas, emailDestino);
        }
        
        return estadisticas;
    }

    // ========================================
    // LISTAR CLIENTES
    // ========================================

    /**
     * Lista todos los clientes activos con paginación.
     * Incluye el cálculo de fecha probable de fallecimiento para cada cliente.
     * 
     * @param pageable Configuración de paginación
     * @return Página con los clientes y sus datos calculados
     */
    @Cacheable(value = "clientes")
    public Page<ClienteResponseDTO> listarClientes(Pageable pageable) {
        log.info("Listando clientes - Página: {}, Tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        // 1. Obtener clientes activos paginados
        Page<Cliente> clientesPage = clienteRepository.findByActivoTrue(pageable);

        // 2. Convertir cada cliente a DTO con cálculos
        Page<ClienteResponseDTO> responseDTO = clientesPage.map(this::convertirAResponseDTO);

        log.info("Retornando {} clientes de {} totales",
                responseDTO.getNumberOfElements(), responseDTO.getTotalElements());

        return responseDTO;
    }

    /**
     * Lista TODOS los clientes activos sin paginación.
     * Útil para el endpoint que requiere la lista completa.
     * 
     * @return Lista de todos los clientes activos
     */
    public List<ClienteResponseDTO> listarTodosLosClientes() {
        log.info("Listando todos los clientes activos");

        List<Cliente> clientes = clienteRepository.findByActivoTrueOrderByFechaRegistroDesc();

        List<ClienteResponseDTO> response = clientes.stream()
                .map(this::convertirAResponseDTO)
                .collect(Collectors.toList());

        log.info("Retornando {} clientes totales", response.size());

        return response;
    }

    // ========================================
    // BUSCAR CLIENTE POR ID
    // ========================================

    /**
     * Busca un cliente por su ID.
     * 
     * @param id ID del cliente a buscar
     * @return DTO con la información del cliente
     * @throws ResourceNotFoundException si el cliente no existe
     */
    public ClienteResponseDTO obtenerClientePorId(Long id) {
        log.info("Buscando cliente con ID: {}", id);

        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> {
                    log.error("Cliente no encontrado con ID: {}", id);
                    return new ResourceNotFoundException("Cliente no encontrado con ID: " + id);
                });

        return convertirAResponseDTO(cliente);
    }

    // ========================================
    // ACTUALIZAR CLIENTE
    // ========================================

    /**
     * Actualizar cliente con auditoría y email opcional.
     */
    @CacheEvict(value = {"estadisticas", "clientes"}, allEntries = true)
    public ClienteResponseDTO actualizarCliente(Long id, ClienteRequestDTO request, String emailDestino) {
        String usuarioActualizador = authService.getUsuarioActual();
        log.info("Usuario {} actualizando cliente con ID: {}", usuarioActualizador, id);
        
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        
        validarCoherenciaEdadFechaNacimiento(request.getEdad(), request.getFechaNacimiento());
        
        // Actualizar campos
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setEdad(request.getEdad());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setActualizadoPor(usuarioActualizador);  // Registrar quién actualizó
        
        cliente = clienteRepository.save(cliente);
        log.info("Cliente {} actualizado exitosamente por {}", id, usuarioActualizador);
        
        ClienteResponseDTO response = convertirAResponseDTO(cliente);
        
        // Enviar notificación si se especificó email
        if (emailDestino != null && !emailDestino.isEmpty() && emailService != null) {
            emailService.enviarNotificacionActualizacion(response, usuarioActualizador, emailDestino);
        }
        
        return response;
    }
    
    /**
     * Sobrecarga para mantener compatibilidad sin email
     */
    public ClienteResponseDTO actualizarCliente(Long id, ClienteRequestDTO request) {
        return actualizarCliente(id, request, null);
    }

    // ========================================
    // ELIMINAR CLIENTE (SOFT DELETE)
    // ========================================

    /**
     * Eliminar cliente con notificación opcional.
     */
    @CacheEvict(value = {"estadisticas", "clientes"}, allEntries = true)
    public void eliminarCliente(Long id, String emailDestino) {
        String usuarioEliminador = authService.getUsuarioActual();
        log.info("Usuario {} eliminando cliente con ID: {}", usuarioEliminador, id);
        
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        
        // Soft delete
        cliente.setActivo(false);
        cliente.setActualizadoPor(usuarioEliminador);  // Registrar quién eliminó
        clienteRepository.save(cliente);
        
        log.info("Cliente {} eliminado (soft delete) exitosamente por {}", id, usuarioEliminador);
        
        // Enviar notificación si se especificó email
        if (emailDestino != null && !emailDestino.isEmpty() && emailService != null) {
            emailService.enviarNotificacionEliminacion(id, usuarioEliminador, emailDestino);
        }
    }
    
    /**
     * Sobrecarga para mantener compatibilidad sin email
     */
    public void eliminarCliente(Long id) {
        eliminarCliente(id, null);
    }

    // ========================================
    // MÉTODOS AUXILIARES PRIVADOS
    // ========================================

    /**
     * Convierte una entidad Cliente a ClienteResponseDTO.
     * Incluye el cálculo de fecha probable de fallecimiento.
     */
    private ClienteResponseDTO convertirAResponseDTO(Cliente cliente) {
        ClienteResponseDTO dto = ClienteResponseDTO.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .apellido(cliente.getApellido())
                .edad(cliente.getEdad())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .fechaRegistro(cliente.getFechaRegistro())
                .build();

        // Calcular fecha probable de fallecimiento
        LocalDate fechaProbableFallecimiento = cliente.getFechaNacimiento().plusYears(esperanzaVida);
        dto.setFechaProbableFallecimiento(fechaProbableFallecimiento);

        // Calcular campos derivados
        dto.calcularCamposDerivados();

        return dto;
    }

    /**
     * Valida que la edad sea coherente con la fecha de nacimiento.
     */
    private void validarCoherenciaEdadFechaNacimiento(Integer edad, LocalDate fechaNacimiento) {
        int edadCalculada = Period.between(fechaNacimiento, LocalDate.now()).getYears();

        // Permitir una diferencia de 1 año (por si el cumpleaños es pronto)
        if (Math.abs(edadCalculada - edad) > 1) {
            throw new BusinessException(
                    String.format("La edad (%d) no es coherente con la fecha de nacimiento. Edad esperada: %d",
                            edad, edadCalculada));
        }
    }

    /**
     * Calcula el promedio de una lista de edades.
     */
    private double calcularPromedio(List<Integer> edades) {
        return edades.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calcula la desviación estándar de las edades.
     */
    private double calcularDesviacionEstandar(List<Integer> edades, double promedio) {
        if (edades.size() <= 1) {
            return 0.0;
        }

        double sumaCuadrados = edades.stream()
                .mapToDouble(edad -> Math.pow(edad - promedio, 2))
                .sum();

        return Math.sqrt(sumaCuadrados / (edades.size() - 1));
    }

    /**
     * Calcula la mediana de las edades.
     */
    private double calcularMediana(List<Integer> edades) {
        List<Integer> edadesOrdenadas = new ArrayList<>(edades);
        Collections.sort(edadesOrdenadas);

        int size = edadesOrdenadas.size();
        if (size % 2 == 0) {
            return (edadesOrdenadas.get(size / 2 - 1) + edadesOrdenadas.get(size / 2)) / 2.0;
        } else {
            return edadesOrdenadas.get(size / 2);
        }
    }

    /**
     * Calcula la distribución de clientes por rango de edad.
     */
    private Map<String, Long> calcularDistribucionPorRangoEdad(List<Integer> edades) {
        return edades.stream()
                .collect(Collectors.groupingBy(
                        edad -> {
                            if (edad < 18)
                                return "0-17";
                            else if (edad < 30)
                                return "18-29";
                            else if (edad < 45)
                                return "30-44";
                            else if (edad < 60)
                                return "45-59";
                            else if (edad < 75)
                                return "60-74";
                            else
                                return "75+";
                        },
                        TreeMap::new, // Para mantener orden
                        Collectors.counting()));
    }

    @Async
    public CompletableFuture<String> procesarClientesEnLote(List<ClienteRequestDTO> clientes) {
        log.info("Iniciando procesamiento asíncrono de {} clientes", clientes.size());

        int exitosos = 0;
        int fallidos = 0;

        for (ClienteRequestDTO dto : clientes) {
            try {
                crearCliente(dto);
                exitosos++;
            } catch (Exception e) {
                log.error("Error creando cliente: {}", e.getMessage());
                fallidos++;
            }
        }

        String resultado = String.format(
                "Procesamiento completado: %d exitosos, %d fallidos de %d total",
                exitosos, fallidos, clientes.size());

        // Enviar email con resumen
        emailService.enviarNotificacionError("procesarClientesEnLote", resultado);

        return CompletableFuture.completedFuture(resultado);
    }

    /**
     * Crea múltiples clientes con notificación por email opcional.
     */
    @Transactional
    @CacheEvict(value = {"estadisticas", "clientes"}, allEntries = true)
    public BatchResponseDTO crearClientesMasivo(List<ClienteRequestDTO> clientesRequest, String emailDestino) {
        log.info("Iniciando creación masiva de {} clientes", clientesRequest.size());
        
        String usuarioCreador = authService.getUsuarioActual();
        log.info("Usuario {} ejecutando creación masiva", usuarioCreador);
        
        List<ClienteResponseDTO> clientesCreados = new ArrayList<>();
        List<ErrorDetalleDTO> errores = new ArrayList<>();
        
        int indice = 0;
        for (ClienteRequestDTO request : clientesRequest) {
            try {
                ClienteResponseDTO clienteCreado = crearClienteIndividual(request);
                clientesCreados.add(clienteCreado);
                log.debug("Cliente {} creado exitosamente en batch", indice);
                
            } catch (BusinessException e) {
                ErrorDetalleDTO error = ErrorDetalleDTO.builder()
                        .indice(indice)
                        .nombre(request.getNombre())
                        .apellido(request.getApellido())
                        .error(e.getMessage())
                        .build();
                
                errores.add(error);
                log.warn("Error creando cliente {} en batch: {}", indice, e.getMessage());
                
            } catch (Exception e) {
                ErrorDetalleDTO error = ErrorDetalleDTO.builder()
                        .indice(indice)
                        .nombre(request.getNombre())
                        .apellido(request.getApellido())
                        .error("Error inesperado: " + e.getMessage())
                        .build();
                
                errores.add(error);
                log.error("Error inesperado creando cliente {} en batch", indice, e);
            }
            
            indice++;
        }
        
        BatchResponseDTO resultado = BatchResponseDTO.builder()
                .total(clientesRequest.size())
                .exitosos(clientesCreados.size())
                .fallidos(errores.size())
                .fechaProcesamiento(LocalDateTime.now())
                .clientesCreados(clientesCreados)
                .errores(errores)
                .build();
        
        log.info("Creación masiva completada por {}: {} exitosos, {} fallidos de {} total",
                usuarioCreador, resultado.getExitosos(), resultado.getFallidos(), resultado.getTotal());
        
        // Enviar notificación si se especificó email
        if (emailDestino != null && !emailDestino.isEmpty() && emailService != null) {
            enviarNotificacionBatchConEmail(resultado, usuarioCreador, emailDestino);
        }
        
        return resultado;
    }

    
    
    /**
     * Método interno para crear un cliente individual sin notificación.
     * Se usa en el procesamiento batch para evitar múltiples emails.
     */
    private ClienteResponseDTO crearClienteIndividual(ClienteRequestDTO request) {
        log.debug("Creando cliente individual en batch: {} {}", 
                request.getNombre(), request.getApellido());
        
        // Validar coherencia entre edad y fecha de nacimiento
        validarCoherenciaEdadFechaNacimiento(request.getEdad(), request.getFechaNacimiento());
        
        // Verificar si ya existe
        boolean clienteExiste = clienteRepository.existsByNombreAndApellidoAndFechaNacimiento(
            request.getNombre(),
            request.getApellido(),
            request.getFechaNacimiento()
        );
        
        if (clienteExiste) {
            throw new BusinessException(String.format(
                "Cliente duplicado: %s %s con fecha nacimiento %s",
                request.getNombre(), request.getApellido(), request.getFechaNacimiento()
            ));
        }
        
        // Crear y guardar
        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .edad(request.getEdad())
                .fechaNacimiento(request.getFechaNacimiento())
                .build();
        
        cliente = clienteRepository.save(cliente);
        
        return convertirAResponseDTO(cliente);
    }
    
   /**
     * Enviar notificación de batch con detalles.
     */
    private void enviarNotificacionBatchConEmail(BatchResponseDTO resultado, String usuarioCreador, String emailDestino) {
        try {
            StringBuilder mensaje = new StringBuilder();
            mensaje.append("Procesamiento Batch Completado\n");
            mensaje.append("================================\n\n");
            mensaje.append("Ejecutado por: ").append(usuarioCreador).append("\n");
            mensaje.append("Total procesados: ").append(resultado.getTotal()).append("\n");
            mensaje.append("Exitosos: ").append(resultado.getExitosos()).append("\n");
            mensaje.append("Fallidos: ").append(resultado.getFallidos()).append("\n");
            mensaje.append("Fecha: ").append(resultado.getFechaProcesamiento()).append("\n\n");
            
            if (resultado.getExitosos() > 0) {
                mensaje.append("CLIENTES CREADOS EXITOSAMENTE:\n");
                mensaje.append("-------------------------------\n");
                for (ClienteResponseDTO cliente : resultado.getClientesCreados()) {
                    mensaje.append(String.format("- ID: %d | %s %s | Edad: %d\n",
                            cliente.getId(), cliente.getNombre(), cliente.getApellido(), cliente.getEdad()));
                }
            }
            
            if (resultado.getFallidos() > 0) {
                mensaje.append("\nERRORES ENCONTRADOS:\n");
                mensaje.append("--------------------\n");
                for (ErrorDetalleDTO error : resultado.getErrores()) {
                    mensaje.append(String.format("- Índice %d | %s %s | Error: %s\n",
                            error.getIndice(), error.getNombre(), error.getApellido(), error.getError()));
                }
            }
            
            emailService.enviarNotificacionPersonalizada(
                emailDestino,
                "Creación Masiva de Clientes - Resumen",
                mensaje.toString()
            );
            
        } catch (Exception e) {
            log.error("Error enviando notificación batch: {}", e.getMessage());
        }
    }
    
    /**
     * Método asíncrono alternativo para procesamiento en background.
     * Útil para grandes volúmenes de datos.
     */
    @Async
    public CompletableFuture<BatchResponseDTO> crearClientesMasivoAsync(
            List<ClienteRequestDTO> clientesRequest) {
        
        log.info("Iniciando procesamiento ASÍNCRONO de {} clientes", clientesRequest.size());
        
        // Llamar al método sin email (null)
        BatchResponseDTO resultado = crearClientesMasivo(clientesRequest, null);
        
        return CompletableFuture.completedFuture(resultado);
    }
    
    /**
     * Método asíncrono con email destino.
     */
    @Async
    public CompletableFuture<BatchResponseDTO> crearClientesMasivoAsync(
            List<ClienteRequestDTO> clientesRequest,
            String emailDestino) {
        
        log.info("Iniciando procesamiento ASÍNCRONO de {} clientes con notificación a {}", 
                clientesRequest.size(), emailDestino);
        
        BatchResponseDTO resultado = crearClientesMasivo(clientesRequest, emailDestino);
        
        return CompletableFuture.completedFuture(resultado);
    }
    
    /**
     * Valida un batch de clientes antes de procesarlos.
     * Útil para pre-validación.
     */
    public List<String> validarBatch(List<ClienteRequestDTO> clientes) {
        List<String> erroresValidacion = new ArrayList<>();
        
        for (int i = 0; i < clientes.size(); i++) {
            ClienteRequestDTO cliente = clientes.get(i);
            
            // Validar coherencia edad/fecha
            try {
                validarCoherenciaEdadFechaNacimiento(
                    cliente.getEdad(), 
                    cliente.getFechaNacimiento()
                );
            } catch (BusinessException e) {
                erroresValidacion.add(String.format(
                    "Cliente %d (%s %s): %s",
                    i, cliente.getNombre(), cliente.getApellido(), e.getMessage()
                ));
            }
            
            // Verificar duplicados
            if (clienteRepository.existsByNombreAndApellidoAndFechaNacimiento(
                    cliente.getNombre(),
                    cliente.getApellido(),
                    cliente.getFechaNacimiento())) {
                
                erroresValidacion.add(String.format(
                    "Cliente %d (%s %s): Ya existe en el sistema",
                    i, cliente.getNombre(), cliente.getApellido()
                ));
            }
        }
        
        return erroresValidacion;
    }

    /**
     * Enviar notificación de creación con información del usuario.
     */
    private void enviarNotificacionCreacion(ClienteResponseDTO cliente, 
                                           String usuarioCreador, 
                                           String emailDestino) {
        if (emailService != null) {
            String mensaje = String.format(
                "Cliente creado exitosamente:\n" +
                "ID: %d\n" +
                "Nombre: %s %s\n" +
                "Edad: %d años\n" +
                "Creado por: %s\n" +
                "Fecha: %s",
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getEdad(),
                usuarioCreador,
                LocalDateTime.now()
            );
            
            emailService.enviarNotificacionPersonalizada(
                emailDestino,
                "Cliente Creado - " + cliente.getNombre() + " " + cliente.getApellido(),
                mensaje
            );
        }
    }
}
