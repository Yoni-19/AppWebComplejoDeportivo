package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;

import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.model.enums.TipoDeporte;

public record CanchaResponse(
        Integer id,
        String nombre,
        TipoDeporte deporte,
        BigDecimal precioPorHora,
        boolean activa
) {
    public static CanchaResponse toResponse(Cancha cancha) {
        return new CanchaResponse(
                cancha.getId(),
                cancha.getNombre(),
                cancha.getDeporte(),
                cancha.getPrecioPorHora(),
                cancha.isActiva()
        );
    }
}
