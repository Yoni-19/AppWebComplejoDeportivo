package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;

import com.proyectosw.sreservas.model.Implemento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta o edicion de un implemento del catalogo. No hay stock: solo nombre,
 * valor de reposicion sugerido y si esta disponible para reservar.
 */
public record ImplementoRequest(
        @NotBlank(message = "El nombre del implemento es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
        String nombre,

        @NotNull(message = "El valor de reposicion es obligatorio")
        @DecimalMin(value = "0.0", message = "El valor de reposicion no puede ser negativo")
        BigDecimal valorReposicion,

        boolean activo
) {
    public Implemento toEntity() {
        Implemento implemento = new Implemento();
        implemento.setNombre(nombre.trim());
        implemento.setValorReposicion(valorReposicion);
        implemento.setActivo(activo);
        return implemento;
    }
}
