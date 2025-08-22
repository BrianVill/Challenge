package com.challenge.challenge_backend.Service;

import com.challenge.challenge_backend.DTOs.Request.ClienteRequestDTO;
import com.challenge.challenge_backend.DTOs.Response.ClienteResponseDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio que contiene la lógica de negocio para la gestión de clientes.
 * 
 * Implementa las operaciones CRUD y cálculos estadísticos requeridos.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j  // Para logging
public class ClienteService {
    
    private final ClienteRepository clienteRepository;
    
    @Value("${app.cliente.esperanza-vida:75}")
    private int esperanzaVida;  // Configurable desde application.properties
    
    // ========================================
    // CREAR CLIENTE
    // ========================================
    
    /**
     * Crea un nuevo cliente en el sistema.
     * 
     * @param request DTO con los datos del cliente a crear
     * @return DTO con la información del cliente creado
     * @throws BusinessException si el cliente ya existe o hay datos inválidos
     */
    @CacheEvict(value = {"estadisticas", "clientes"}, allEntries = true)
    public ClienteResponseDTO crearCliente(ClienteRequestDTO request) {
        log.info("Iniciando creación de cliente: {} {}", request.getNombre(), request.getApellido());
        
        // 1. Validar coherencia entre edad y fecha de nacimiento
        validarCoherenciaEdadFechaNacimiento(request.getEdad(), request.getFechaNacimiento());
        
        // 2. Verificar si ya existe un cliente similar (evitar duplicados)
        boolean clienteExiste = clienteRepository.existsByNombreAndApellidoAndFechaNacimiento(
            request.getNombre(),
            request.getApellido(),
            request.getFechaNacimiento()
        );
        
        if (clienteExiste) {
            log.warn("Intento de crear cliente duplicado: {} {}", request.getNombre(), request.getApellido());
            throw new BusinessException("Ya existe un cliente con los mismos datos");
        }
        
        // 3. Crear la entidad Cliente
        Cliente cliente = Cliente.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .edad(request.getEdad())
                .fechaNacimiento(request.getFechaNacimiento())
                .build();
        
        // 4. Guardar en la base de datos
        cliente = clienteRepository.save(cliente);
        log.info("Cliente creado exitosamente con ID: {}", cliente.getId());
        
        // 5. Convertir a DTO de respuesta
        ClienteResponseDTO response = convertirAResponseDTO(cliente);
        
        return response;
    }
    
    // ========================================
    // OBTENER ESTADÍSTICAS
    // ========================================
    
    /**
     * Calcula las estadísticas de todos los clientes activos.
     * Incluye promedio de edad y desviación estándar.
     * 
     * @return DTO con las estadísticas calculadas
     */
    @Cacheable(value = "estadisticas", key = "'general'")
    public EstadisticasClienteDTO calcularEstadisticas() {
        log.info("Calculando estadísticas de clientes");
        
        // 1. Obtener todos los clientes activos
        List<Cliente> clientesActivos = clienteRepository.findByActivoTrue();
        
        // 2. Si no hay clientes, retornar estadísticas vacías
        if (clientesActivos.isEmpty()) {
            log.info("No hay clientes activos para calcular estadísticas");
            return EstadisticasClienteDTO.builder()
                    .totalClientes(0L)
                    .promedioEdad(0.0)
                    .desviacionEstandar(0.0)
                    .mensaje("No hay clientes registrados en el sistema")
                    .build();
        }
        
        // 3. Extraer las edades para los cálculos
        List<Integer> edades = clientesActivos.stream()
                .map(Cliente::getEdad)
                .collect(Collectors.toList());
        
        // 4. Calcular estadísticas básicas
        double promedio = calcularPromedio(edades);
        double desviacionEstandar = calcularDesviacionEstandar(edades, promedio);
        
        // 5. Calcular estadísticas adicionales
        Integer edadMinima = Collections.min(edades);
        Integer edadMaxima = Collections.max(edades);
        Double mediana = calcularMediana(edades);
        Map<String, Long> distribucion = calcularDistribucionPorRangoEdad(edades);
        
        // 6. Construir y retornar el DTO
        EstadisticasClienteDTO estadisticas = EstadisticasClienteDTO.builder()
                .totalClientes((long) clientesActivos.size())
                .promedioEdad(promedio)
                .desviacionEstandar(desviacionEstandar)
                .edadMinima(edadMinima)
                .edadMaxima(edadMaxima)
                .medianaEdad(mediana)
                .distribucionPorRangoEdad(distribucion)
                .mensaje(String.format("Estadísticas calculadas para %d clientes activos", clientesActivos.size()))
                .build();
        
        log.info("Estadísticas calculadas exitosamente: {} clientes, promedio edad: {}", 
                clientesActivos.size(), promedio);
        
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
     * Actualiza los datos de un cliente existente.
     * 
     * @param id ID del cliente a actualizar
     * @param request DTO con los nuevos datos
     * @return DTO con la información actualizada
     * @throws ResourceNotFoundException si el cliente no existe
     */
    @CacheEvict(value = {"estadisticas", "clientes"}, allEntries = true)
    public ClienteResponseDTO actualizarCliente(Long id, ClienteRequestDTO request) {
        log.info("Actualizando cliente con ID: {}", id);
        
        // 1. Buscar el cliente
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        
        // 2. Validar coherencia de datos
        validarCoherenciaEdadFechaNacimiento(request.getEdad(), request.getFechaNacimiento());
        
        // 3. Actualizar los campos
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setEdad(request.getEdad());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        
        // 4. Guardar cambios
        cliente = clienteRepository.save(cliente);
        log.info("Cliente actualizado exitosamente");
        
        // 5. Retornar DTO actualizado
        return convertirAResponseDTO(cliente);
    }
    
    // ========================================
    // ELIMINAR CLIENTE (SOFT DELETE)
    // ========================================
    
    /**
     * Elimina lógicamente un cliente (soft delete).
     * 
     * @param id ID del cliente a eliminar
     * @throws ResourceNotFoundException si el cliente no existe
     */
    @CacheEvict(value = {"estadisticas", "clientes"}, allEntries = true)
    public void eliminarCliente(Long id) {
        log.info("Eliminando cliente con ID: {}", id);
        
        Cliente cliente = clienteRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + id));
        
        // Soft delete - solo marcar como inactivo
        cliente.setActivo(false);
        clienteRepository.save(cliente);
        
        log.info("Cliente eliminado (soft delete) exitosamente");
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
                    edad, edadCalculada)
            );
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
            return (edadesOrdenadas.get(size/2 - 1) + edadesOrdenadas.get(size/2)) / 2.0;
        } else {
            return edadesOrdenadas.get(size/2);
        }
    }
    
    /**
     * Calcula la distribución de clientes por rango de edad.
     */
    private Map<String, Long> calcularDistribucionPorRangoEdad(List<Integer> edades) {
        return edades.stream()
                .collect(Collectors.groupingBy(
                    edad -> {
                        if (edad < 18) return "0-17";
                        else if (edad < 30) return "18-29";
                        else if (edad < 45) return "30-44";
                        else if (edad < 60) return "45-59";
                        else if (edad < 75) return "60-74";
                        else return "75+";
                    },
                    TreeMap::new,  // Para mantener orden
                    Collectors.counting()
                ));
    }
}
