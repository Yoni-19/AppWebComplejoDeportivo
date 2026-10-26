package com.proyectosw.sreservas.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.EstadoReserva;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.repository.CanchaRepository;
import com.proyectosw.sreservas.repository.ReservaRepository;
import com.proyectosw.sreservas.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
public class ReservaService {
    private static final Collection<EstadoReserva> ESTADOS_QUE_OCUPAN =
            List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA, EstadoReserva.COMPLETADA);

    private final ReservaRepository repository;
    private final CanchaRepository canchaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurrentUserService currentUser;

    public ReservaService(final ReservaRepository repository, final CanchaRepository canchaRepository,
            final UsuarioRepository usuarioRepository, final CurrentUserService currentUser) {
        this.repository = repository;
        this.canchaRepository = canchaRepository;
        this.usuarioRepository = usuarioRepository;
        this.currentUser = currentUser;
    }

    /** ADMIN y RECEPCIONISTA ven todas las reservas; CLIENTE solo las propias. */
    public List<Reserva> listarTodos(){
        Usuario actual = currentUser.get();
        if (Rol.CLIENTE.equals(actual.getRol())) {
            return repository.findByUsuario_Id(actual.getId());
        }
        return repository.findAll();
    }

    public Optional<Reserva> buscarPorId(Integer id) {
        Optional<Reserva> reserva = repository.findById(id);
        reserva.ifPresent(this::verificarPropiedad);
        return reserva;
    }

    @Transactional 
    public Reserva crear(Reserva reserva){
        if (reserva.getCancha() == null || reserva.getCancha().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reserva debe indicar una cancha");
        }

        Usuario actual = currentUser.get();
        boolean esCliente = Rol.CLIENTE.equals(actual.getRol());
        if (!esCliente && (reserva.getUsuario() == null || reserva.getUsuario().getId() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reserva debe indicar un usuario");
        }

        Cancha cancha = canchaRepository.findById(reserva.getCancha().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la cancha con id " + reserva.getCancha().getId()));
        if (!cancha.isActiva()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cancha " + cancha.getNombre() + " está inactiva y no se puede reservar");
        }
        reserva.setCancha(cancha);

        if (esCliente) {
            // El cliente reserva siempre para si mismo: se ignora el usuarioId del cuerpo.
            reserva.setUsuario(actual);
        } else {
            Usuario usuario = usuarioRepository.findById(reserva.getUsuario().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No existe el usuario con id " + reserva.getUsuario().getId()));
            reserva.setUsuario(usuario);
        }

        if (!reserva.getFechaHoraFin().isAfter(reserva.getFechaHoraInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de fin debe ser posterior a la de inicio");
        }
        verificarSolapamiento(cancha.getId(), null,
                reserva.getFechaHoraInicio(), reserva.getFechaHoraFin());

        reserva.setEstadoReserva(EstadoReserva.PENDIENTE);
        recalcularMonto(reserva);

        return repository.save(reserva);
    }

    @Transactional 
    public Reserva actualizar(Integer id, Reserva datos){
        Reserva existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la reserva con id " + id));

        // Un CLIENTE solo reprograma sus propias reservas; el personal puede con todas.
        verificarPropiedad(existente);

        if (existente.getEstadoReserva() == EstadoReserva.COMPLETADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede reprogramar una reserva completada");
        }

        if (datos.getFechaHoraInicio() != null) existente.setFechaHoraInicio(datos.getFechaHoraInicio());
        if (datos.getFechaHoraFin() != null)     existente.setFechaHoraFin(datos.getFechaHoraFin());

        if (datos.getCancha() != null && datos.getCancha().getId() != null) {
            Cancha cancha = canchaRepository.findById(datos.getCancha().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la cancha con id " + datos.getCancha().getId()));
            if (!cancha.isActiva()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cancha " + cancha.getNombre() + " está inactiva y no se puede reservar");
            }

            existente.setCancha(cancha);
        }

        if (!existente.getFechaHoraFin().isAfter(existente.getFechaHoraInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de fin debe ser posterior a la de inicio");
        }
        verificarSolapamiento(existente.getCancha().getId(), id,
                existente.getFechaHoraInicio(), existente.getFechaHoraFin());

        recalcularMonto(existente);

        return repository.save(existente);      
    }

    @Transactional
    public void eliminar(Integer id) {
        Reserva existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe reserva con id " + id));

        // Un CLIENTE solo cancela sus propias reservas.
        verificarPropiedad(existente);

        if (existente.getEstadoReserva() == EstadoReserva.COMPLETADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cancelar una reserva completada");
        }
        repository.delete(existente);
    }

    /** Un cliente no puede ver reservas ajenas; el personal si. */
    private void verificarPropiedad(Reserva reserva) {
        Usuario actual = currentUser.get();
        if (Rol.CLIENTE.equals(actual.getRol())
                && !reserva.getUsuario().getId().equals(actual.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La reserva pertenece a otro usuario");
        }
    }

    private void verificarSolapamiento(Integer canchaId, Integer reservaPropiaId,
                                       LocalDateTime inicio, LocalDateTime fin) {
        boolean ocupada = repository.existeSolapamiento(canchaId, reservaPropiaId,
                ESTADOS_QUE_OCUPAN, inicio, fin);
        if (ocupada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cancha ya está reservada en ese horario");
        }
    }

    private void recalcularMonto(Reserva reserva) {
        BigDecimal precioHora = reserva.getCancha().getPrecioPorHora();
        long minutos = Duration.between(
                reserva.getFechaHoraInicio(),
                reserva.getFechaHoraFin()).toMinutes();
        long horas = (minutos + 59) / 60;
        reserva.setMontoTotal(precioHora.multiply(BigDecimal.valueOf(horas)));
    }
}
