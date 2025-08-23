package com.challenge.challenge_backend.Security;

import com.challenge.challenge_backend.Exception.TokenException;
import com.challenge.challenge_backend.Exception.TokenException.TokenErrorType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio para manejo de tokens JWT.
 */
@Service
@Slf4j
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}")  // Default 24 horas
    private Long jwtExpiration;
    
    // La clave secreta para firmar/verificar tokens
    private SecretKey key;
    
    /**
     * Inicializa la clave secreta con manejo de errores.
     */
    @PostConstruct
    public void init() {
        try {
            // Intentar decodificar como Base64
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT Secret key inicializada correctamente (Base64)");
            
        } catch (Exception e) {
            log.warn("El secret no es Base64 válido, usando el string directo: {}", e.getMessage());
            
            try {
                // Si falla, usar el string directamente pero asegurar 256 bits mínimo
                String paddedSecret = secretKey;
                
                // Si el secret es muy corto, lo extendemos
                while (paddedSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
                    paddedSecret = paddedSecret + secretKey;
                }
                
                // Tomar solo los primeros 32 bytes (256 bits)
                byte[] keyBytes = paddedSecret.getBytes(StandardCharsets.UTF_8);
                if (keyBytes.length > 32) {
                    byte[] trimmedKey = new byte[32];
                    System.arraycopy(keyBytes, 0, trimmedKey, 0, 32);
                    keyBytes = trimmedKey;
                }
                
                this.key = Keys.hmacShaKeyFor(keyBytes);
                log.info("JWT Secret key inicializada usando string directo (convertido a 256 bits)");
                
            } catch (Exception ex) {
                log.error("Error fatal inicializando JWT secret key", ex);
                throw new RuntimeException("No se pudo inicializar la clave JWT. Verifica jwt.secret en application.properties", ex);
            }
        }
    }
    
    /**
     * Genera un token JWT para un usuario.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }
    
    /**
     * Genera un token JWT con claims adicionales.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)  
                .compact();
    }
    
    /**
     * Valida si un token es válido para un usuario.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error validando token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extrae el username del token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extrae un claim específico del token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Extrae todos los claims del token.
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)  // Usamos la SecretKey
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenException("Token expirado", TokenErrorType.EXPIRED);
        } catch (MalformedJwtException e) {
            throw new TokenException("Token malformado", TokenErrorType.MALFORMED);
        } catch (JwtException e) {
            throw new TokenException("Token inválido", TokenErrorType.INVALID);
        }
    }
    
    /**
     * Verifica si el token ha expirado.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    /**
     * Extrae la fecha de expiración del token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Obtiene el tiempo de expiración configurado.
     */
    public Long getExpirationTime() {
        return jwtExpiration;
    }
    
    /**
     * Valida si un header de autorización tiene el formato correcto.
     */
    public boolean isValidAuthHeader(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ");
    }
    
    /**
     * Extrae el token del header de autorización.
     */
    public String extractTokenFromHeader(String authHeader) {
        if (isValidAuthHeader(authHeader)) {
            return authHeader.substring(7);
        }
        return null;
    }
}