# 🚀 Challenge Backend - Sistema de Gestión de Clientes

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-green.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![AWS](https://img.shields.io/badge/AWS-Deployed-yellow.svg)](https://aws.amazon.com/)
[![Status](https://img.shields.io/badge/Status-Live-success.svg)](http://challenge-backend.us-east-1.elasticbeanstalk.com/api/health)

## 📝 Descripción del Proyecto

API REST desarrollada en Spring Boot para la gestión integral de clientes, implementando cálculos estadísticos avanzados y proyecciones demográficas basadas en esperanza de vida. El sistema está desplegado en AWS utilizando arquitectura cloud-native para garantizar escalabilidad y disponibilidad.

### 🎯 Objetivos Cumplidos

- ✅ Crear nuevos clientes con validación completa de datos
- ✅ Calcular KPIs estadísticos (promedio de edad y desviación estándar)
- ✅ Listar clientes con fecha probable de fallecimiento
- ✅ Sistema de autenticación JWT robusto
- ✅ Despliegue en producción en AWS

## 🌐 URL de Producción

**Base URL:** `http://challenge-backend.us-east-1.elasticbeanstalk.com`

### 📋 Endpoints Principales

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/auth/login` | Iniciar sesión y obtener token JWT | No |
| POST | `/api/creacliente` | Crear nuevo cliente | Sí (JWT) |
| GET | `/api/kpideclientes` | Obtener estadísticas de clientes | Sí (JWT) |
| GET | `/api/listclientes` | Listar todos los clientes con fecha probable de fallecimiento | Sí (JWT) |
| GET | `/api/health` | Verificar estado del servicio | No |

## 🛠️ Stack Tecnológico

### Backend Core
- **Java 17** - Lenguaje de programación principal
- **Spring Boot 3.5.5** - Framework empresarial
- **Spring Security + JWT** - Seguridad y autenticación stateless
- **Spring Data JPA/Hibernate** - ORM y persistencia
- **Maven** - Gestión de dependencias y build

### Base de Datos
- **MySQL 8.0** - RDBMS principal
- **AWS RDS** - Base de datos gestionada en la nube
- **HikariCP** - Pool de conexiones de alto rendimiento

### Infraestructura Cloud (AWS)
- **Elastic Beanstalk** - PaaS para despliegue automático
- **EC2 (t2.micro)** - Compute instances
- **RDS MySQL** - Base de datos gestionada
- **CloudWatch** - Monitoreo y logging
- **VPC & Security Groups** - Networking y seguridad

### Herramientas Adicionales
- **Lombok** - Reducción de código boilerplate
- **MapStruct** - Mapeo automático DTO-Entity
- **Caffeine Cache** - Sistema de caché en memoria
- **JavaMail** - Notificaciones asíncronas
- **Swagger/OpenAPI** - Documentación automática de API

## 🏗️ Decisiones Arquitectónicas

### 1. **Arquitectura en Capas (Layered Architecture)**
```
Controller → Service → Repository → Database
    ↓           ↓          ↓
   DTOs     Business    Entities
            Logic
```

**Justificación:** Separación clara de responsabilidades, facilitando mantenimiento y testing. Cada capa tiene un propósito específico y bien definido.

### 2. **Patrón DTO (Data Transfer Object)**
- **Request DTOs**: Validación de entrada con Jakarta Validation
- **Response DTOs**: Formateo consistente de respuestas
- **Mapeo automático**: MapStruct para conversión Entity↔DTO

**Justificación:** Desacoplar la capa de presentación del modelo de dominio, permitiendo evolución independiente y validación robusta.

### 3. **Repository Pattern con Spring Data JPA**
```java
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // Queries personalizadas con método naming convention
    List<Cliente> findByActivoTrue();
    Optional<Cliente> findByIdAndActivoTrue(Long id);
}
```

**Justificación:** Abstracción del acceso a datos, queries type-safe y reducción de código boilerplate.

### 4. **Service Layer Pattern**
- Toda la lógica de negocio centralizada
- Transaccionalidad declarativa con `@Transactional`
- Separación entre lógica de negocio y acceso a datos

**Justificación:** Reutilización de lógica, facilidad para testing y punto único de mantenimiento.

### 5. **Global Exception Handling**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    // Manejo centralizado de excepciones
}
```

**Justificación:** Respuestas de error consistentes, logging centralizado y mejor experiencia de usuario.

### 6. **Stateless Authentication con JWT**
- No sesiones en servidor
- Token auto-contenido con claims
- Refresh token no implementado (simplicidad)

**Justificación:** Escalabilidad horizontal, ideal para microservicios y APIs REST.

### 7. **Soft Delete Pattern**
```java
@Column(name = "activo")
private Boolean activo = true;
```

**Justificación:** Preservación de datos históricos, cumplimiento regulatorio y posibilidad de auditoría.

### 8. **Async Processing**
```java
@Async
public CompletableFuture<Boolean> enviarNotificacionClienteCreado(ClienteResponseDTO cliente)
```

**Justificación:** Mejora de performance, operaciones no bloqueantes para tareas secundarias.

## 📦 Estructura del Proyecto

```
challenge-backend/
├── src/
│   ├── main/
│   │   ├── java/com/challenge/challenge_backend/
│   │   │   ├── Config/              # Configuraciones Spring
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── AsyncConfig.java
│   │   │   │   └── DataInitializer.java
│   │   │   ├── Controller/          # REST Controllers
│   │   │   │   ├── ClienteController.java
│   │   │   │   └── AuthController.java
│   │   │   ├── DTOs/                # Data Transfer Objects
│   │   │   │   ├── Request/
│   │   │   │   │   ├── ClienteRequestDTO.java
│   │   │   │   │   └── LoginRequestDTO.java
│   │   │   │   └── Response/
│   │   │   │       ├── ClienteResponseDTO.java
│   │   │   │       └── EstadisticasClienteDTO.java
│   │   │   ├── Exception/           # Excepciones personalizadas
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── BusinessException.java
│   │   │   ├── Models/              # Entidades JPA
│   │   │   │   ├── Cliente.java
│   │   │   │   └── Usuario.java
│   │   │   ├── Repository/          # Acceso a datos
│   │   │   │   ├── ClienteRepository.java
│   │   │   │   └── UsuarioRepository.java
│   │   │   ├── Security/            # Seguridad JWT
│   │   │   │   ├── JwtService.java
│   │   │   │   └── JwtAuthenticationFilter.java
│   │   │   └── Service/             # Lógica de negocio
│   │   │       ├── ClienteService.java
│   │   │       ├── AuthService.java
│   │   │       └── EmailService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-prod.properties
│   └── test/
├── pom.xml
└── README.md
```

## 🚀 Cómo Ejecutar el Proyecto

### Prerrequisitos
- Java 17 o superior
- Maven 3.6+
- MySQL 8.0
- Git

### Instalación Local

1. **Clonar el repositorio:**
```bash
git clone https://github.com/BrianVill/challenge-backend.git
cd challenge-backend
```

2. **Configurar base de datos MySQL:**
```sql
CREATE DATABASE challengedb;
CREATE USER 'challenge_user'@'localhost' IDENTIFIED BY 'Challenge2025!';
GRANT ALL PRIVILEGES ON challengedb.* TO 'challenge_user'@'localhost';
FLUSH PRIVILEGES;
```

3. **Configurar application.properties:**
```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/challengedb?useSSL=false&serverTimezone=UTC
spring.datasource.username=challenge_user
spring.datasource.password=Challenge2025!
server.port=8080
```

4. **Compilar y ejecutar:**
```bash
# Compilar el proyecto
mvn clean package

# Opción 1: Ejecutar con Maven
mvn spring-boot:run

# Opción 2: Ejecutar el JAR generado
java -jar target/challenge-backend-0.0.1-SNAPSHOT.jar
```

5. **Verificar que está funcionando:**
```bash
curl http://localhost:8080/api/health
# Respuesta esperada: {"success":true,"message":"Cliente API está funcionando correctamente","data":"Service is running"}
```

### Ejecución con Docker (Opcional)

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/challenge-backend-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

```bash
docker build -t challenge-backend .
docker run -p 8080:8080 challenge-backend
```

## 🧪 Cómo Realizar Pruebas

### 📬 Colección de Postman

**[📥 Acceder a la Colección de Postman](https://stride-6501.postman.co/workspace/Stride-Workspace~e4b38bb6-971f-4e11-8477-ac0bbe411549/request/35081456-26b391ce-0e92-44d8-baf3-caee8a29350b?action=share&creator=35081456&ctx=documentation)**

La colección incluye:
- Variables de entorno preconfiguradas
- Todos los endpoints documentados
- Ejemplos de requests y responses
- Tests automáticos para validación

### Guía de Pruebas Paso a Paso

#### 1. **Obtener Token de Autenticación**
```bash
curl -X POST http://challenge-backend.us-east-1.elasticbeanstalk.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@challenge.com",
    "password": "Admin123!"
  }'
```

Guardar el token devuelto para usar en los siguientes requests.

#### 2. **Crear un Cliente**
```bash
curl -X POST http://challenge-backend.us-east-1.elasticbeanstalk.com/api/creacliente \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN_OBTENIDO}" \
  -d '{
    "nombre": "Juan",
    "apellido": "Pérez",
    "edad": 35,
    "fechaNacimiento": "1989-03-15"
  }'
```

#### 3. **Obtener Estadísticas (KPIs)**
```bash
curl -X GET http://challenge-backend.us-east-1.elasticbeanstalk.com/api/kpideclientes \
  -H "Authorization: Bearer {TOKEN_OBTENIDO}"
```

#### 4. **Listar Todos los Clientes**
```bash
curl -X GET http://challenge-backend.us-east-1.elasticbeanstalk.com/api/listclientes \
  -H "Authorization: Bearer {TOKEN_OBTENIDO}"
```

### Credenciales de Prueba

| Rol | Email | Password |
|-----|-------|----------|
| ADMIN | admin@challenge.com | Admin123! |

⚠️ **Nota:** El token JWT tiene una duración de 24 horas.

## 📊 Patrones de Diseño Implementados

### 1. **Singleton Pattern**
- Beans de Spring (@Service, @Repository, @Component)
- Configuración única de SecurityConfig y JwtService

### 2. **Factory Pattern**
- JwtService para creación de tokens
- ResponseEntity builders en controllers

### 3. **Builder Pattern**
```java
Cliente cliente = Cliente.builder()
    .nombre(request.getNombre())
    .apellido(request.getApellido())
    .edad(request.getEdad())
    .build();
```

### 4. **Strategy Pattern**
- Diferentes estrategias de autenticación (JWT vs Session)
- Implementación flexible mediante interfaces

### 5. **Template Method Pattern**
- JpaRepository proporciona template methods
- Métodos personalizados extienden funcionalidad base

### 6. **Dependency Injection Pattern**
- Inyección de dependencias mediante @Autowired y constructor injection
- Inversión de control con Spring IoC Container

## 🔐 Seguridad Implementada

### Autenticación y Autorización
- **JWT (JSON Web Tokens)** para autenticación stateless
- **BCrypt** para hash de contraseñas
- **Spring Security** para manejo de roles y permisos
- **CORS** configurado para permitir requests desde frontend

### Validaciones
- Validación de DTOs con Jakarta Bean Validation
- Validación de coherencia edad/fecha de nacimiento
- Prevención de duplicados
- Sanitización de inputs

### Mejores Prácticas
- Secrets en variables de entorno
- HTTPS en producción (preparado para SSL)
- Soft delete para preservar integridad referencial
- Auditoría de operaciones sensibles

## 🌟 Características Adicionales Implementadas

1. **Sistema de Autenticación Completo**
   - Login con JWT
   - Roles (ADMIN/USER)
   - Refresh token preparado

2. **Operaciones Batch**
   - Creación masiva de clientes
   - Procesamiento asíncrono

3. **Sistema de Notificaciones**
   - Email automático al crear clientes
   - Notificaciones de estadísticas

4. **Caché en Memoria**
   - Caffeine Cache para optimización
   - TTL configurable

5. **Paginación y Filtrado**
   - Soporte para grandes volúmenes
   - Ordenamiento dinámico

6. **Auditoría Completa**
   - Registro de creación y modificación
   - Trazabilidad de operaciones

7. **Health Checks**
   - Endpoint de monitoreo
   - Integración con AWS CloudWatch

## 📈 Información Relevante Adicional

### Performance
- Pool de conexiones optimizado con HikariCP
- Queries optimizadas con índices en MySQL
- Caché para reducir carga en BD
- Procesamiento asíncrono para operaciones pesadas

### Escalabilidad
- Arquitectura stateless permite escalado horizontal
- Preparado para microservicios
- Compatible con load balancers
- Auto-scaling configurado en AWS

### Monitoreo
- Logs estructurados con SLF4J
- Métricas con Spring Actuator
- CloudWatch integration en AWS
- Health checks automáticos

## 👥 Autor

**Brian Villalva**
- GitHub: [@BrianVill](https://github.com/BrianVill)
- LinkedIn: [Brian Villalva](https://www.linkedin.com/in/brian-villalva-76b822238/)

---

**Estado del Servicio:** 🟢 ONLINE

**URL de Producción:** http://challenge-backend.us-east-1.elasticbeanstalk.com
