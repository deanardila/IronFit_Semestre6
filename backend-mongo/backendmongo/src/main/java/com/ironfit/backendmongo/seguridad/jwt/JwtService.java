package com.ironfit.backendmongo.seguridad.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final Key llave;
    private final int expiracionMinutos;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-minutes}") int expiracionMinutos
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("security.jwt.secret debe tener al menos 32 caracteres");
        }
        this.llave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(String userId, String correo, List<String> roles) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plusSeconds(expiracionMinutos * 60L);

        return Jwts.builder()
                .setSubject(userId)
                .claim("correo", correo)
                .claim("roles", roles)
                .setIssuedAt(Date.from(ahora))
                .setExpiration(Date.from(expira))
                .signWith(llave, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parsear(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(llave)
                .build()
                .parseClaimsJws(token);
    }

    public boolean esValido(String token) {
        try {
            parsear(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}