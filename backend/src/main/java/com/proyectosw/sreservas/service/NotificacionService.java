package com.proyectosw.sreservas.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.model.Notificacion;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.TipoNotificacion;
import com.proyectosw.sreservas.repository.NotificacionRepository;

import jakarta.transaction.Transactional;

/**
 * Bandeja de avisos internos.
 *
 * Los observers de entrenamientos y mantenimientos escriben aca; el usuario
 * autenticado solo puede leer y marcar sus propias notificaciones.
 */
@Service
public class NotificacionService {

    private final NotificacionRepository repository;
    private final CurrentUserService currentUser;

    public NotificacionService(final NotificacionRepository repository,
            final CurrentUserService currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    /** Alta de un aviso; la usan los observers, no el controller. */
    public void enviar(Usuario destinatario, TipoNotificacion tipo, String titulo,
            String mensaje, String referencia) {
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatario(destinatario);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setReferencia(referencia);
        notificacion.setLeida(false);
        notificacion.setFechaCreacion(LocalDateTime.now());
        repository.save(notificacion);
    }

    public List<Notificacion> listarPropias() {
        return repository.findByDestinatario_IdOrderByFechaCreacionDesc(currentUser.get().getId());
    }

    public long contarNoLeidas() {
        return repository.countByDestinatario_IdAndLeidaFalse(currentUser.get().getId());
    }

    @Transactional
    public Notificacion marcarLeida(Integer id) {
        Usuario actual = currentUser.get();
        Notificacion notificacion = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la notificacion con id " + id));
        if (!notificacion.getDestinatario().getId().equals(actual.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La notificacion pertenece a otro usuario");
        }
        notificacion.setLeida(true);
        return repository.save(notificacion);
    }

    @Transactional
    public void marcarTodasLeidas() {
        List<Notificacion> pendientes = repository
                .findByDestinatario_IdAndLeidaFalse(currentUser.get().getId());
        pendientes.forEach(notificacion -> notificacion.setLeida(true));
    }
}
