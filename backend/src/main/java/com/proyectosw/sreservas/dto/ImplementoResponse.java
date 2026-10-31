package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;

import com.proyectosw.sreservas.model.Implemento;

/**
 * Implemento del catalogo tal como lo ve el frontend: sin stock, porque el
 * inventario lo administra el complejo por fuera del sistema.
 */
public record ImplementoResponse(
        Integer id,
        String nombre,
        BigDecimal valorReposicion,
        boolean activo
) {
    public static ImplementoResponse toResponse(Implemento implemento) {
        return new ImplementoResponse(
                implemento.getId(),
                implemento.getNombre(),
                implemento.getValorReposicion(),
                implemento.isActivo()
        );
    }
}
