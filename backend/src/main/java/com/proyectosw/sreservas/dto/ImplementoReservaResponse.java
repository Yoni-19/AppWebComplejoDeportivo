package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Estado de los implementos de una reserva para la pantalla de utileria:
 * cuanto se pidio, cuanto se entrego, cuanto volvio y cuanto falta.
 *
 * Todo se calcula cruzando las lineas IMPLEMENTO del detalle con los movimientos
 * ENTREGA/DEVOLUCION. No hay stock: esto es de una reserva puntual.
 *
 * {@code valorReposicion} permite a la pantalla anticipar el cargo por danio o
 * perdida antes de registrar la devolucion; es el mismo precio que usaria el
 * backend si no se indica uno.
 */
public record ImplementoReservaResponse(
        Integer reservaId,
        List<LineaImplemento> lineas
) {
    public record LineaImplemento(
            Integer implementoId,
            String nombre,
            int pedidos,
            int entregados,
            int devueltos,
            int pendientes,
            BigDecimal valorReposicion
    ) {
    }
}
