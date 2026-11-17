package com.proyectosw.sreservas.observer;

import java.util.List;

import com.proyectosw.sreservas.model.Mantenimiento;
import com.proyectosw.sreservas.model.Reserva;

/**
 * Observer de los mantenimientos.
 *
 * Al abrir un bloqueo se avisa a recepcion con las reservas afectadas; al
 * resolverlo se avisa que la cancha vuelve a estar disponible.
 */
public interface MantenimientoObserver {
    void alAbrir(Mantenimiento mantenimiento, List<Reserva> reservasAfectadas);

    void alResolver(Mantenimiento mantenimiento);
}
