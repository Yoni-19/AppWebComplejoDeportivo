package com.proyectosw.sreservas.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationDays;

    public JwtService(
            @Value("${application.jwt.secret-key}") String secretKey,
            @Value("${application.jwt.token-expiration-after-days}") long expirationDays) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationDays = expirationDays;
    }

    public String generateToken(String correo) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationDays * 24 * 60 * 60 * 1000L);
        return Jwts.builder()
                .subject(correo)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public String extractCorreo(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isValidToken(String token) {
        try {
            extractCorreo(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}