package com.proyectosw.sreservas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.proyectosw.sreservas.model.Entrenamiento;
import com.proyectosw.sreservas.model.Ocupacion;
import com.proyectosw.sreservas.model.enums.EstadoEntrenamiento;

/**
 * Vista de un entrenamiento para el frontend.
 *
 * {@code inscriptos} cuenta solo las inscripciones vigentes; {@code inscripto}
 * indica si el usuario autenticado ya ocupa un cupo. {@code cupoDisponible} es
 * la resta que usa la UI para deshabilitar el boton de inscripcion.
 */
public record EntrenamientoResponse(
        Integer id,
        String titulo,
        String entrenador,
        String descripcion,
        BigDecimal precio,
        int cupoMaximo,
        long inscriptos,
        int cupoDisponible,
        EstadoEntrenamiento estado,
        Integer canchaId,
        String canchaNombre,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        boolean inscripto
) {
    public static EntrenamientoResponse toResponse(Entrenamiento entrenamiento, long inscriptos,
            boolean inscripto) {
        Ocupacion ocupacion = entrenamiento.getOcupacion();
        int disponibles = Math.max(entrenamiento.getCupoMaximo() - (int) inscriptos, 0);
        return new EntrenamientoResponse(
                entrenamiento.getId(),
                entrenamiento.getTitulo(),
                entrenamiento.getEntrenador(),
                entrenamiento.getDescripcion(),
                entrenamiento.getPrecio(),
                entrenamiento.getCupoMaximo(),
                inscriptos,
                disponibles,
                entrenamiento.getEstado(),
                ocupacion.getCancha().getId(),
                ocupacion.getCancha().getNombre(),
                ocupacion.getFechaHoraInicio(),
                ocupacion.getFechaHoraFin(),
                inscripto
        );
    }
}
