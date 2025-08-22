package com.challenge.challenge_backend.Security;

import com.challenge.challenge_backend.Exception.TokenException;
import com.challenge.challenge_backend.Exception.TokenException.TokenErrorType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio para manejo de tokens JWT.
 * 
 * Gestiona la creación, validación y extracción de información de tokens JWT.
 * OPCIONAL - Solo necesario si implementas autenticación con JWT.
 */
@Service
@Slf4j
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration}")
    private Long jwtExpiration; // en milisegundos
    
    // ========================================
    // GENERACIÓN DE TOKENS
    // ========================================
    
    /**
     * Genera un token JWT para un usuario.
     * 
     * @param userDetails Detalles del usuario
     * @return Token JWT generado
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }
    
    /**
     * Genera un token JWT con claims adicionales.
     * 
     * @param extraClaims Claims adicionales para incluir en el token
     * @param userDetails Detalles del usuario
     * @return Token JWT generado
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        log.debug("Generando token JWT para usuario: {}", userDetails.getUsername());
        
        // Agregar información adicional útil
        extraClaims.put("authorities", userDetails.getAuthorities());
        
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }
    
    /**
     * Construye el token JWT.
     */
    /**
     * Construye el token JWT.
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            Long expiration
    ) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        String token = Jwts
                .builder()
                .claims(extraClaims)  // Cambio: usar claims() en lugar de setClaims()
                .subject(userDetails.getUsername())  // Cambio: usar subject() en lugar de setSubject()
                .issuedAt(now)  // Cambio: usar issuedAt() en lugar de setIssuedAt()
                .expiration(expiryDate)  // Cambio: usar expiration() en lugar de setExpiration()
                .signWith(getSignInKey(), Jwts.SIG.HS256)  // Cambio: usar Jwts.SIG.HS256
                .compact();
        
        log.debug("Token generado exitosamente. Expira en: {}", expiryDate);
        
        return token;
    }
    
    // ========================================
    // VALIDACIÓN DE TOKENS
    // ========================================
    
    /**
     * Valida si un token es válido para un usuario.
     * 
     * @param token Token JWT
     * @param userDetails Detalles del usuario
     * @return true si el token es válido, false en caso contrario
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean isValid = (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
            
            if (!isValid) {
                log.warn("Token inválido para usuario: {}", userDetails.getUsername());
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Error validando token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica si un token ha expirado.
     * 
     * @param token Token JWT
     * @return true si el token ha expirado
     */
    private boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        boolean isExpired = expiration.before(new Date());
        
        if (isExpired) {
            log.debug("Token expirado. Fecha de expiración: {}", expiration);
        }
        
        return isExpired;
    }
    
    // ========================================
    // EXTRACCIÓN DE INFORMACIÓN
    // ========================================
    
    /**
     * Extrae el username (email) del token.
     * 
     * @param token Token JWT
     * @return Username contenido en el token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extrae la fecha de expiración del token.
     * 
     * @param token Token JWT
     * @return Fecha de expiración
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extrae un claim específico del token.
     * 
     * @param token Token JWT
     * @param claimsResolver Función para extraer el claim
     * @return Valor del claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extrae todos los claims del token.
     * 
     * @param token Token JWT
     * @return Todos los claims del token
     * @throws TokenException si el token es inválido
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parser()  // Cambio: usar parser() en lugar de parserBuilder()
                    .verifyWith(getSignInKey())  // Cambio: usar verifyWith() en lugar de setSigningKey()
                    .build()
                    .parseSignedClaims(token)  // Cambio: usar parseSignedClaims()
                    .getPayload();  // Cambio: usar getPayload() en lugar de getBody()
                    
        } catch (ExpiredJwtException e) {
            log.error("Token JWT expirado: {}", e.getMessage());
            throw new TokenException("El token ha expirado", TokenErrorType.EXPIRED);
            
        } catch (UnsupportedJwtException e) {
            log.error("Token JWT no soportado: {}", e.getMessage());
            throw new TokenException("Token JWT no soportado", TokenErrorType.UNSUPPORTED);
            
        } catch (MalformedJwtException e) {
            log.error("Token JWT malformado: {}", e.getMessage());
            throw new TokenException("Token JWT malformado", TokenErrorType.MALFORMED);
            
        } catch (SignatureException e) {
            log.error("Firma del token JWT inválida: {}", e.getMessage());
            throw new TokenException("Firma del token inválida", TokenErrorType.SIGNATURE_INVALID);
            
        } catch (IllegalArgumentException e) {
            log.error("Token JWT vacío o nulo: {}", e.getMessage());
            throw new TokenException("Token JWT vacío", TokenErrorType.INVALID);
            
        } catch (Exception e) {
            log.error("Error procesando token JWT: {}", e.getMessage());
            throw new TokenException("Error procesando el token", TokenErrorType.INVALID);
        }
    }
    
    // ========================================
    // MÉTODOS AUXILIARES
    // ========================================
    
    /**
     * Obtiene la clave de firma para los tokens.
     * 
     * @return Clave de firma
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Obtiene el tiempo de expiración configurado.
     * 
     * @return Tiempo de expiración en milisegundos
     */
    public Long getExpirationTime() {
        return jwtExpiration;
    }
    
    /**
     * Valida si un token tiene el formato correcto (Bearer token).
     * 
     * @param authHeader Header de autorización
     * @return true si el formato es válido
     */
    public boolean isValidAuthHeader(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }
    
    /**
     * Extrae el token del header de autorización.
     * 
     * @param authHeader Header de autorización
     * @return Token JWT sin el prefijo "Bearer "
     */
    public String extractTokenFromHeader(String authHeader) {
        if (isValidAuthHeader(authHeader)) {
            return authHeader.substring(7); // Remueve "Bearer "
        }
        return null;
    }
    
    /**
     * Genera un token de refresh (con mayor duración).
     * 
     * @param userDetails Detalles del usuario
     * @return Refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        log.debug("Generando refresh token para usuario: {}", userDetails.getUsername());
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        
        // Refresh token dura 7 días (configurable)
        Long refreshExpiration = jwtExpiration * 7;
        
        return buildToken(claims, userDetails, refreshExpiration);
    }
    
    /**
     * Valida si un token es un refresh token válido.
     * 
     * @param token Token a validar
     * @return true si es un refresh token válido
     */
    public boolean isValidRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String type = (String) claims.get("type");
            return "refresh".equals(type) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
