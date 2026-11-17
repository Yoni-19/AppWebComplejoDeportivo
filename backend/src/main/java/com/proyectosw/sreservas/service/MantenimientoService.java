package com.proyectosw.sreservas.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.dto.MantenimientoRequest;
import com.proyectosw.sreservas.dto.MantenimientoResponse;
import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.model.Mantenimiento;
import com.proyectosw.sreservas.model.Ocupacion;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.EstadoServicio;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.model.enums.TipoOcupacion;
import com.proyectosw.sreservas.observer.MantenimientoObserver;
import com.proyectosw.sreservas.repository.CanchaRepository;
import com.proyectosw.sreservas.repository.MantenimientoRepository;
import com.proyectosw.sreservas.repository.OcupacionRepository;
import com.proyectosw.sreservas.repository.ReservaRepository;

import jakarta.transaction.Transactional;

/**
 * Bloqueos de cancha por mantenimiento.
 *
 * Abrir un bloqueo no cancela reservas por si solo: primero se informan las
 * reservas afectadas (Observer) para que recepcion reprograme o avise a los
 * clientes. Mientras la ocupacion siga activa, ninguna reserva nueva entra en
 * ese horario porque el fin nulo se trata como infinito.
 */
@Service
public class MantenimientoService {

    private final MantenimientoRepository repository;
    private final OcupacionRepository ocupacionRepository;
    private final CanchaRepository canchaRepository;
    private final ReservaRepository reservaRepository;
    private final CurrentUserService currentUser;
    private final List<MantenimientoObserver> observers;

    public MantenimientoService(final MantenimientoRepository repository,
            final OcupacionRepository ocupacionRepository,
            final CanchaRepository canchaRepository,
            final ReservaRepository reservaRepository,
            final CurrentUserService currentUser,
            final List<MantenimientoObserver> observers) {
        this.repository = repository;
        this.ocupacionRepository = ocupacionRepository;
        this.canchaRepository = canchaRepository;
        this.reservaRepository = reservaRepository;
        this.currentUser = currentUser;
        this.observers = observers;
    }

    public List<MantenimientoResponse> listar() {
        return repository.findAllByOrderByResueltoAscOcupacion_FechaHoraInicioDesc().stream()
                .map(MantenimientoResponse::toResponse)
                .toList();
    }

    public MantenimientoResponse buscarPorId(Integer id) {
        return MantenimientoResponse.toResponse(obtener(id));
    }

    /** Abre un bloqueo desde la fecha indicada (o desde ahora) y avisa a recepcion. */
    @Transactional
    public Mantenimiento abrir(MantenimientoRequest request) {
        verificarOperacion();
        Cancha cancha = canchaRepository.findById(request.canchaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la cancha con id " + request.canchaId()));

        if (repository.existsByOcupacion_Cancha_IdAndResueltoFalse(cancha.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cancha ya tiene un mantenimiento abierto");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicio = request.fechaHoraInicio() != null
                ? request.fechaHoraInicio() : ahora;
        // Solo cuentan las reservas que todavia no terminaron cuando se abre el bloqueo.
        LocalDateTime desde = inicio.isAfter(ahora) ? inicio : ahora;
        List<Reserva> afectadas = reservaRepository.findVigentesDesde(cancha.getId(),
                List.of(EstadoServicio.CONFIRMADA, EstadoServicio.EN_CURSO), desde);

        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setCancha(cancha);
        ocupacion.setFechaHoraInicio(inicio);
        ocupacion.setFechaHoraFin(null);
        ocupacion.setTipo(TipoOcupacion.MANTENIMIENTO);
        ocupacion.setActiva(true);
        ocupacionRepository.save(ocupacion);

        Usuario actual = currentUser.get();
        Mantenimiento mantenimiento = new Mantenimiento();
        mantenimiento.setOcupacion(ocupacion);
        mantenimiento.setMotivo(request.motivo());
        mantenimiento.setResuelto(false);
        mantenimiento.setFechaCreacion(LocalDateTime.now());
        mantenimiento.setReservasAfectadas(afectadas.size());
        mantenimiento.setReportadoPor(actual);

        Mantenimiento guardado = repository.save(mantenimiento);
        notificarApertura(guardado, afectadas);
        return guardado;
    }

    /** Cierra el bloqueo: registra la solución, libera la cancha y avisa a recepción. */
    @Transactional
    public Mantenimiento resolver(Integer id, String solucion) {
        verificarOperacion();
        if (solucion == null || solucion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La solución es obligatoria");
        }
        Mantenimiento mantenimiento = obtener(id);
        if (mantenimiento.isResuelto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El mantenimiento ya esta resuelto");
        }

        mantenimiento.setResuelto(true);
        mantenimiento.setSolucion(solucion.trim());
        LocalDateTime ahora = LocalDateTime.now();
        mantenimiento.setFechaResolucion(ahora);
        Ocupacion ocupacion = mantenimiento.getOcupacion();
        ocupacion.setActiva(false);
        // El fin real del bloqueo queda en el historial; si se resolvio antes de
        // que empezara, se registra el mismo inicio para no invertir el rango.
        ocupacion.setFechaHoraFin(ahora.isBefore(ocupacion.getFechaHoraInicio())
                ? ocupacion.getFechaHoraInicio() : ahora);

        Mantenimiento guardado = repository.save(mantenimiento);
        for (MantenimientoObserver observer : observers) {
            observer.alResolver(guardado);
        }
        return guardado;
    }

    private Mantenimiento obtener(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el mantenimiento con id " + id));
    }

    private void notificarApertura(Mantenimiento mantenimiento, List<Reserva> afectadas) {
        for (MantenimientoObserver observer : observers) {
            observer.alAbrir(mantenimiento, afectadas);
        }
    }

    private void verificarOperacion() {
        Rol rol = currentUser.get().getRol();
        if (rol != Rol.ADMIN && rol != Rol.MANTENIMIENTO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo mantenimiento o el admin abren y resuelven bloqueos");
        }
    }
}
