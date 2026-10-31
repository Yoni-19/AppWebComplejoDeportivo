package com.proyectosw.sreservas.dto;

import java.util.List;

/**
 * Estado de los implementos de una reserva para la pantalla de utileria:
 * cuanto se pidio, cuanto se entrego, cuanto volvio y cuanto falta.
 *
 * Todo se calcula cruzando las lineas IMPLEMENTO del detalle con los movimientos
 * ENTREGA/DEVOLUCION. No hay stock: esto es de una reserva puntual.
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
            int pendientes
    ) {
    }
}
