package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;

import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.model.enums.TipoDeporte;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CanchaRequest(
        @NotBlank(message = "El nombre de la cancha es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotNull(message = "El deporte es obligatorio")
        TipoDeporte deporte,

        @NotNull(message = "El precio por hora es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio por hora debe ser mayor a cero")
        BigDecimal precioPorHora,

        boolean activa
) {
    public Cancha toEntity() {
        Cancha cancha = new Cancha();
        cancha.setNombre(nombre);
        cancha.setDeporte(deporte);
        cancha.setPrecioPorHora(precioPorHora);
        cancha.setActiva(activa);
        return cancha;
    }
}
