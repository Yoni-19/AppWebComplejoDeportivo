package com.proyectosw.sreservas.dto;

/**
 * JWT login payload. Serialized as { "token": "..." } for API compatibility.
 */
public record AuthResponse(
        String token
) {
}
