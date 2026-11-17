package com.proyectosw.sreservas.dto;

import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.Mantenimiento;
import com.proyectosw.sreservas.model.Ocupacion;

/** Vista de un mantenimiento y de la cancha que quedo bloqueada. */
public record MantenimientoResponse(
        Integer id,
        Integer canchaId,
        String canchaNombre,
        String motivo,
        String solucion,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        boolean resuelto,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaResolucion,
        int reservasAfectadas,
        String reportadoPorNombre
) {
    public static MantenimientoResponse toResponse(Mantenimiento mantenimiento) {
        Ocupacion ocupacion = mantenimiento.getOcupacion();
        String reportado = null;
        if (mantenimiento.getReportadoPor() != null) {
            reportado = mantenimiento.getReportadoPor().getNombres() + " "
                    + mantenimiento.getReportadoPor().getApellidos();
        }
        return new MantenimientoResponse(
                mantenimiento.getId(),
                ocupacion.getCancha().getId(),
                ocupacion.getCancha().getNombre(),
                mantenimiento.getMotivo(),
                mantenimiento.getSolucion(),
                ocupacion.getFechaHoraInicio(),
                ocupacion.getFechaHoraFin(),
                mantenimiento.isResuelto(),
                mantenimiento.getFechaCreacion(),
                mantenimiento.getFechaResolucion(),
                mantenimiento.getReservasAfectadas(),
                reportado
        );
    }
}
