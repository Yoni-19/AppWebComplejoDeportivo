package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.Ocupacion;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.EstadoCuenta;
import com.proyectosw.sreservas.model.enums.EstadoServicio;

/**
 * Reservation view. The court and the time range are read from the reservation's
 * {@link Ocupacion}; the nested {@code Usuario} and {@code Cancha} are flattened to
 * id + name to avoid serialization cycles and hash leakage.
 *
 * The account fields ({@code cuentaId}, {@code estadoCuenta}, {@code saldo}) let the
 * frontend show what is still owed without an extra request; {@code detalle} is the
 * order-like breakdown of the charges.
 */
public record ReservaResponse(
        Integer id,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        EstadoServicio estado,
        BigDecimal montoTotal,
        Integer canchaId,
        String canchaNombre,
        Integer usuarioId,
        String usuarioNombre,
        Integer cuentaId,
        EstadoCuenta estadoCuenta,
        BigDecimal saldo,
        List<DetalleReservaResponse> detalle
) {
    public static ReservaResponse toResponse(Reserva reserva) {
        Ocupacion ocupacion = reserva.getOcupacion();
        Cuenta cuenta = reserva.getCuenta();
        return new ReservaResponse(
                reserva.getId(),
                ocupacion.getFechaHoraInicio(),
                ocupacion.getFechaHoraFin(),
                reserva.getEstado(),
                reserva.getMontoTotal(),
                ocupacion.getCancha().getId(),
                ocupacion.getCancha().getNombre(),
                reserva.getUsuario().getId(),
                reserva.getUsuario().getNombres() + " " + reserva.getUsuario().getApellidos(),
                cuenta != null ? cuenta.getId() : null,
                cuenta != null ? cuenta.getEstado() : null,
                cuenta != null ? cuenta.getSaldo() : null,
                reserva.getDetalles().stream().map(DetalleReservaResponse::toResponse).toList()
        );
    }
}
