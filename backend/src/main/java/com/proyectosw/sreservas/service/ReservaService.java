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
import com.proyectosw.sreservas.factory.PagoFactory;
import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.DetalleReserva;
import com.proyectosw.sreservas.model.Ocupacion;
import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.EstadoServicio;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.model.enums.TipoDetalle;
import com.proyectosw.sreservas.model.enums.TipoOcupacion;
import com.proyectosw.sreservas.repository.CanchaRepository;
import com.proyectosw.sreservas.repository.CuentaRepository;
import com.proyectosw.sreservas.repository.DetalleReservaRepository;
import com.proyectosw.sreservas.repository.OcupacionRepository;
import com.proyectosw.sreservas.repository.PagoRepository;
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
    private final DetalleReservaRepository detalleRepository;
    private final CuentaRepository cuentaRepository;
    private final PagoRepository pagoRepository;
    private final CuentaService cuentaService;

    public ReservaService(final ReservaRepository repository, final OcupacionRepository ocupacionRepository,
            final CanchaRepository canchaRepository, final UsuarioRepository usuarioRepository,
            final CurrentUserService currentUser, final DetalleReservaRepository detalleRepository,
            final CuentaRepository cuentaRepository, final PagoRepository pagoRepository,
            final CuentaService cuentaService) {
        this.repository = repository;
        this.ocupacionRepository = ocupacionRepository;
        this.canchaRepository = canchaRepository;
        this.usuarioRepository = usuarioRepository;
        this.currentUser = currentUser;
        this.detalleRepository = detalleRepository;
        this.cuentaRepository = cuentaRepository;
        this.pagoRepository = pagoRepository;
        this.cuentaService = cuentaService;
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

    /** Cuenta de una reserva, con la misma regla de propiedad que la reserva. */
    public Cuenta buscarCuenta(Integer reservaId) {
        Reserva reserva = repository.findById(reservaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la reserva con id " + reservaId));
        verificarPropiedad(reserva);
        Cuenta cuenta = reserva.getCuenta();
        if (cuenta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "La reserva " + reservaId + " no tiene cuenta");
        }
        return cuenta;
    }

    @Transactional
    public Reserva crear(ReservaRequest request) {
        if (request.metodoPago() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El método de pago del anticipo es obligatorio");
        }

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
        reserva.setMontoTotal(calcularMonto(cancha, request.fechaHoraInicio(), request.fechaHoraFin()));
        reserva = repository.save(reserva);

        DetalleReserva linea = lineaCancha(reserva, cancha, request.fechaHoraInicio(), request.fechaHoraFin());
        detalleRepository.save(linea);
        reserva.getDetalles().add(linea);

        Cuenta cuenta = cuentaService.crear(reserva, reserva.getMontoTotal());
        reserva.setCuenta(cuenta);

        Pago anticipo = PagoFactory.anticipo(cuenta, request.metodoPago());
        cuentaService.registrarPago(cuenta, anticipo);

        return reserva;
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
        if (existente.getEstado() == EstadoServicio.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede reprogramar una reserva cancelada");
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

        BigDecimal monto = calcularMonto(cancha, inicio, fin);
        existente.setMontoTotal(monto);
        actualizarLineaCancha(existente, cancha, inicio, fin);

        Cuenta cuenta = existente.getCuenta();
        if (cuenta != null) {
            cuentaService.recalcularTotal(cuenta, monto);
        }

        return repository.save(existente);
    }

    /**
     * Cancela una reserva confirmada. A diferencia de eliminar, conserva la
     * reserva, el detalle y los pagos: solo libera el turno y anula la cuenta.
     */
    @Transactional
    public Reserva cancelar(Integer id) {
        Reserva existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la reserva con id " + id));

        // Un CLIENTE solo cancela sus propias reservas; el personal puede con todas.
        verificarPropiedad(existente);

        if (existente.getEstado() != EstadoServicio.CONFIRMADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede cancelar una reserva confirmada");
        }

        existente.setEstado(EstadoServicio.CANCELADA);

        // La ocupacion no se borra: queda inactiva para conservar el historial.
        // El control de solapamiento solo mira las activas, asi que el turno se libera.
        Ocupacion ocupacion = existente.getOcupacion();
        if (ocupacion != null) {
            ocupacion.setActiva(false);
        }

        Cuenta cuenta = existente.getCuenta();
        if (cuenta != null) {
            cuentaService.anular(cuenta);
        }

        return repository.save(existente);
    }

    /**
     * Borrado fisico, reservado al ADMIN para limpiar datos cargados por error.
     * La operacion de negocio para dar de baja un servicio es {@link #cancelar(Integer)}.
     */
    @Transactional
    public void eliminar(Integer id) {
        Reserva existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe reserva con id " + id));

        // Defensa en profundidad: hoy solo el ADMIN llega aca, pero la regla de
        // propiedad se mantiene por si la matriz de seguridad cambia.
        verificarPropiedad(existente);

        if (existente.getEstado() == EstadoServicio.COMPLETADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar una reserva completada");
        }

        // Primero los hijos (pagos, cuenta, detalle), despues la reserva y la ocupacion.
        // Hay que cortar la referencia inversa antes de borrar la cuenta: si no,
        // Hibernate intenta persistir una cuenta ya eliminada al hacer flush.
        Cuenta cuenta = existente.getCuenta();
        existente.setCuenta(null);
        existente.getDetalles().clear();
        if (cuenta != null) {
            pagoRepository.deleteByCuenta_Id(cuenta.getId());
            cuentaRepository.delete(cuenta);
        }
        detalleRepository.deleteByReserva_Id(existente.getId());

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

    private DetalleReserva lineaCancha(Reserva reserva, Cancha cancha,
                                       LocalDateTime inicio, LocalDateTime fin) {
        DetalleReserva linea = new DetalleReserva();
        linea.setReserva(reserva);
        linea.setTipo(TipoDetalle.CANCHA);
        aplicarDatosCancha(linea, cancha, inicio, fin);
        return linea;
    }

    private void actualizarLineaCancha(Reserva reserva, Cancha cancha,
                                       LocalDateTime inicio, LocalDateTime fin) {
        DetalleReserva linea = detalleRepository
                .findFirstByReserva_IdAndTipo(reserva.getId(), TipoDetalle.CANCHA)
                .orElseGet(() -> {
                    DetalleReserva nueva = new DetalleReserva();
                    nueva.setReserva(reserva);
                    nueva.setTipo(TipoDetalle.CANCHA);
                    reserva.getDetalles().add(nueva);
                    return nueva;
                });
        aplicarDatosCancha(linea, cancha, inicio, fin);
        detalleRepository.save(linea);
    }

    private void aplicarDatosCancha(DetalleReserva linea, Cancha cancha,
                                    LocalDateTime inicio, LocalDateTime fin) {
        long horas = horasIniciadas(inicio, fin);
        linea.setDescripcion(cancha.getNombre());
        linea.setCantidad((int) horas);
        linea.setPrecioUnitario(cancha.getPrecioPorHora());
        linea.setSubtotal(cancha.getPrecioPorHora().multiply(BigDecimal.valueOf(horas)));
    }

    private BigDecimal calcularMonto(Cancha cancha, LocalDateTime inicio, LocalDateTime fin) {
        return cancha.getPrecioPorHora()
                .multiply(BigDecimal.valueOf(horasIniciadas(inicio, fin)));
    }

    /** Monto por hora iniciada: 90 minutos son 2 horas. */
    private long horasIniciadas(LocalDateTime inicio, LocalDateTime fin) {
        long minutos = Duration.between(inicio, fin).toMinutes();
        return (minutos + 59) / 60;
    }
}
