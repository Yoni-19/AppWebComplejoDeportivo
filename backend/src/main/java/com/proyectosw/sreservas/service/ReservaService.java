package com.proyectosw.sreservas.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.dto.ReservaRequest;
import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.model.Ocupacion;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.EstadoServicio;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.model.enums.TipoOcupacion;
import com.proyectosw.sreservas.repository.CanchaRepository;
import com.proyectosw.sreservas.repository.OcupacionRepository;
import com.proyectosw.sreservas.repository.ReservaRepository;
import com.proyectosw.sreservas.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
public class ReservaService {
    private final ReservaRepository repository;
    private final OcupacionRepository ocupacionRepository;
    private final CanchaRepository canchaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CurrentUserService currentUser;

    public ReservaService(final ReservaRepository repository, final OcupacionRepository ocupacionRepository,
            final CanchaRepository canchaRepository, final UsuarioRepository usuarioRepository,
            final CurrentUserService currentUser) {
        this.repository = repository;
        this.ocupacionRepository = ocupacionRepository;
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
    public Reserva crear(ReservaRequest request) {
        Cancha cancha = canchaRepository.findById(request.canchaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la cancha con id " + request.canchaId()));
        if (!cancha.isActiva()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cancha " + cancha.getNombre() + " esta inactiva y no se puede reservar");
        }

        Usuario actual = currentUser.get();
        Usuario usuario;
        if (Rol.CLIENTE.equals(actual.getRol())) {
            // El cliente reserva siempre para si mismo: se ignora el usuarioId del cuerpo.
            usuario = actual;
        } else {
            if (request.usuarioId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reserva debe indicar un usuario");
            }
            usuario = usuarioRepository.findById(request.usuarioId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No existe el usuario con id " + request.usuarioId()));
        }

        if (!request.fechaHoraFin().isAfter(request.fechaHoraInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de fin debe ser posterior a la de inicio");
        }
        verificarSolapamiento(cancha.getId(), null, request.fechaHoraInicio(), request.fechaHoraFin());

        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setCancha(cancha);
        ocupacion.setFechaHoraInicio(request.fechaHoraInicio());
        ocupacion.setFechaHoraFin(request.fechaHoraFin());
        ocupacion.setTipo(TipoOcupacion.RESERVA);
        ocupacion.setActiva(true);
        ocupacionRepository.save(ocupacion);

        Reserva reserva = new Reserva();
        reserva.setOcupacion(ocupacion);
        reserva.setUsuario(usuario);
        reserva.setEstado(EstadoServicio.CONFIRMADA);
        recalcularMonto(reserva);

        return repository.save(reserva);
    }

    @Transactional
    public Reserva actualizar(Integer id, ReservaRequest request) {
        Reserva existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la reserva con id " + id));

        // Un CLIENTE solo reprograma sus propias reservas; el personal puede con todas.
        verificarPropiedad(existente);

        if (existente.getEstado() == EstadoServicio.COMPLETADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede reprogramar una reserva completada");
        }

        Ocupacion ocupacion = existente.getOcupacion();

        Cancha cancha = ocupacion.getCancha();
        if (request.canchaId() != null && !request.canchaId().equals(cancha.getId())) {
            cancha = canchaRepository.findById(request.canchaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No existe la cancha con id " + request.canchaId()));
            if (!cancha.isActiva()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La cancha " + cancha.getNombre() + " esta inactiva y no se puede reservar");
            }
        }

        LocalDateTime inicio = request.fechaHoraInicio() != null
                ? request.fechaHoraInicio() : ocupacion.getFechaHoraInicio();
        LocalDateTime fin = request.fechaHoraFin() != null
                ? request.fechaHoraFin() : ocupacion.getFechaHoraFin();

        if (!fin.isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de fin debe ser posterior a la de inicio");
        }
        verificarSolapamiento(cancha.getId(), ocupacion.getId(), inicio, fin);

        ocupacion.setCancha(cancha);
        ocupacion.setFechaHoraInicio(inicio);
        ocupacion.setFechaHoraFin(fin);
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

        if (existente.getEstado() == EstadoServicio.COMPLETADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cancelar una reserva completada");
        }

        // El FK vive en reserva (ocupacion_id): se borra primero la reserva.
        Ocupacion ocupacion = existente.getOcupacion();
        repository.delete(existente);
        repository.flush();
        ocupacionRepository.delete(ocupacion);
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

    private void verificarSolapamiento(Integer canchaId, Integer ocupacionPropiaId,
                                       LocalDateTime inicio, LocalDateTime fin) {
        boolean ocupada = ocupacionRepository.existeSolapamiento(canchaId, ocupacionPropiaId, inicio, fin);
        if (ocupada) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cancha ya esta reservada en ese horario");
        }
    }

    private void recalcularMonto(Reserva reserva) {
        Ocupacion ocupacion = reserva.getOcupacion();
        BigDecimal precioHora = ocupacion.getCancha().getPrecioPorHora();
        long minutos = Duration.between(
                ocupacion.getFechaHoraInicio(),
                ocupacion.getFechaHoraFin()).toMinutes();
        long horas = (minutos + 59) / 60;
        reserva.setMontoTotal(precioHora.multiply(BigDecimal.valueOf(horas)));
    }
}
