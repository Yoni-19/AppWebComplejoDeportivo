package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Devolucion de un implemento entregado.
 *
 * {@code devueltas} incluye las unidades que vuelven danadas: volvieron, pero se
 * cobran. {@code danadas} es un subconjunto de {@code devueltas}. El faltante
 * (entregado - devuelto) se cobra como PERDIDA.
 *
 * Si no se indica precio, el backend usa el valor de reposicion del catalogo.
 */
public record DevolucionImplementoRequest(
        @NotNull(message = "La devolucion debe indicar el implemento")
        Integer implementoId,

        @NotNull(message = "La devolucion debe indicar cuantas unidades volvieron")
        @Min(value = 0, message = "Las unidades devueltas no pueden ser negativas")
        Integer devueltas,

        @Min(value = 0, message = "Las unidades danadas no pueden ser negativas")
        Integer danadas,

        @DecimalMin(value = "0.0", message = "El precio del danio no puede ser negativo")
        BigDecimal precioDanio,

        @DecimalMin(value = "0.0", message = "El precio de la perdida no puede ser negativo")
        BigDecimal precioPerdida
) {
    public int danadasSeguras() {
        return danadas == null ? 0 : danadas;
    }
}
