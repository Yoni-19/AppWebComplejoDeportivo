package com.proyectosw.sreservas.observer;

import java.util.List;

import org.springframework.stereotype.Component;

import com.proyectosw.sreservas.model.Mantenimiento;
import com.proyectosw.sreservas.model.Ocupacion;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.model.enums.TipoNotificacion;
import com.proyectosw.sreservas.repository.UsuarioRepository;
import com.proyectosw.sreservas.service.NotificacionService;

/**
 * Avisa a recepcion y al admin cuando una cancha se bloquea o se libera.
 *
 * La recepcion es quien atiende a los clientes afectados; el admin recibe el
 * mismo aviso como respaldo de gestion.
 */
@Component
public class NotificacionMantenimientoObserver implements MantenimientoObserver {

    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;

    public NotificacionMantenimientoObserver(final NotificacionService notificacionService,
            final UsuarioRepository usuarioRepository) {
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void alAbrir(Mantenimiento mantenimiento, List<Reserva> reservasAfectadas) {
        Ocupacion ocupacion = mantenimiento.getOcupacion();
        String referencia = "MANTENIMIENTO:" + mantenimiento.getId();
        String mensaje = "Se bloqueo " + ocupacion.getCancha().getNombre() + " desde "
                + ocupacion.getFechaHoraInicio() + " por: " + mantenimiento.getMotivo()
                + ". Reservas vigentes afectadas: " + reservasAfectadas.size() + ".";
        enviarATrabajo(TipoNotificacion.MANTENIMIENTO_ABIERTO, "Mantenimiento abierto", mensaje,
                referencia);
    }

    @Override
    public void alResolver(Mantenimiento mantenimiento) {
        Ocupacion ocupacion = mantenimiento.getOcupacion();
        String referencia = "MANTENIMIENTO:" + mantenimiento.getId();
        String mensaje = ocupacion.getCancha().getNombre() + " quedo disponible: "
                + mantenimiento.getMotivo() + " resuelto.";
        enviarATrabajo(TipoNotificacion.MANTENIMIENTO_RESUELTO, "Mantenimiento resuelto", mensaje,
                referencia);
    }

    private void enviarATrabajo(TipoNotificacion tipo, String titulo, String mensaje, String referencia) {
        for (Usuario destinatario : usuarioRepository.findByRolIn(List.of(Rol.ADMIN, Rol.RECEPCIONISTA))) {
            notificacionService.enviar(destinatario, tipo, titulo, mensaje, referencia);
        }
    }
}
