package com.proyectosw.sreservas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.dto.EntrenamientoRequest;
import com.proyectosw.sreservas.dto.EntrenamientoResponse;
import com.proyectosw.sreservas.dto.InscripcionEntrenamientoResponse;
import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.model.Entrenamiento;
import com.proyectosw.sreservas.model.InscripcionEntrenamiento;
import com.proyectosw.sreservas.model.Ocupacion;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.EstadoEntrenamiento;
import com.proyectosw.sreservas.model.enums.EstadoInscripcion;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.model.enums.TipoOcupacion;
import com.proyectosw.sreservas.observer.EntrenamientoObserver;
import com.proyectosw.sreservas.repository.CanchaRepository;
import com.proyectosw.sreservas.repository.EntrenamientoRepository;
import com.proyectosw.sreservas.repository.InscripcionEntrenamientoRepository;
import com.proyectosw.sreservas.repository.OcupacionRepository;

import jakarta.transaction.Transactional;

/**
 * Reglas de negocio de los entrenamientos.
 *
 * La cancha y el horario viven en {@link Ocupacion}, igual que en las reservas:
 * el solapamiento se chequea una sola vez sobre esa tabla. La cancelacion y la
 * reprogramacion disparan los observers para avisar a los inscriptos.
 */
@Service
public class EntrenamientoService {

    private final EntrenamientoRepository repository;
    private final InscripcionEntrenamientoRepository inscripcionRepository;
    private final OcupacionRepository ocupacionRepository;
    private final CanchaRepository canchaRepository;
    private final CurrentUserService currentUser;
    private final List<EntrenamientoObserver> observers;

    public EntrenamientoService(final EntrenamientoRepository repository,
            final InscripcionEntrenamientoRepository inscripcionRepository,
            final OcupacionRepository ocupacionRepository,
            final CanchaRepository canchaRepository,
            final CurrentUserService currentUser,
            final List<EntrenamientoObserver> observers) {
        this.repository = repository;
        this.inscripcionRepository = inscripcionRepository;
        this.ocupacionRepository = ocupacionRepository;
        this.canchaRepository = canchaRepository;
        this.currentUser = currentUser;
        this.observers = observers;
    }

    /** Catalogo completo; cada fila trae cupos y si el usuario ya esta inscripto. */
    public List<EntrenamientoResponse> listar() {
        Usuario actual = currentUser.get();
        return repository.findAllByOrderByOcupacion_FechaHoraInicioAsc().stream()
                .map(entrenamiento -> aResponse(entrenamiento, actual))
                .toList();
    }

    public EntrenamientoResponse buscarPorId(Integer id) {
        Entrenamiento entrenamiento = obtener(id);
        return aResponse(entrenamiento, currentUser.get());
    }

    public List<InscripcionEntrenamientoResponse> listarInscripciones(Integer id) {
        obtener(id);
        return inscripcionRepository.findByEntrenamiento_IdOrderByIdAsc(id).stream()
                .map(InscripcionEntrenamientoResponse::toResponse)
                .toList();
    }

    @Transactional
    public Entrenamiento crear(EntrenamientoRequest request) {
        verificarGestion();
        Cancha cancha = canchaActiva(request.canchaId());
        validarHorario(request.fechaHoraInicio(), request.fechaHoraFin());
        verificarSolapamiento(cancha.getId(), null, request.fechaHoraInicio(), request.fechaHoraFin());

        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setCancha(cancha);
        ocupacion.setFechaHoraInicio(request.fechaHoraInicio());
        ocupacion.setFechaHoraFin(request.fechaHoraFin());
        ocupacion.setTipo(TipoOcupacion.ENTRENAMIENTO);
        ocupacion.setActiva(true);
        ocupacionRepository.save(ocupacion);

        Entrenamiento entrenamiento = new Entrenamiento();
        entrenamiento.setOcupacion(ocupacion);
        aplicarDatos(entrenamiento, request);
        entrenamiento.setEstado(EstadoEntrenamiento.PROGRAMADO);
        return repository.save(entrenamiento);
    }

    /**
     * Reprograma o edita los datos del entrenamiento. Un cambio de cancha u
     * horario avisa a los inscriptos; editar el titulo o el cupo no los molesta.
     */
    @Transactional
    public Entrenamiento actualizar(Integer id, EntrenamientoRequest request) {
        verificarGestion();
        Entrenamiento entrenamiento = obtener(id);
        if (entrenamiento.getEstado() != EstadoEntrenamiento.PROGRAMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede modificar un entrenamiento cancelado");
        }
        if (entrenamiento.getOcupacion().getFechaHoraInicio().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede modificar un entrenamiento que ya comenzo");
        }

        List<InscripcionEntrenamiento> vigentes = inscripcionesVigentes(id);
        if (request.cupoMaximo() < vigentes.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El cupo no puede ser menor a las " + vigentes.size() + " inscripciones vigentes");
        }

        Cancha cancha = canchaActiva(request.canchaId());
        validarHorario(request.fechaHoraInicio(), request.fechaHoraFin());

        Ocupacion ocupacion = entrenamiento.getOcupacion();
        boolean cambioHorario = !request.fechaHoraInicio().equals(ocupacion.getFechaHoraInicio())
                || !request.fechaHoraFin().equals(ocupacion.getFechaHoraFin())
                || !cancha.getId().equals(ocupacion.getCancha().getId());
        if (cambioHorario) {
            verificarSolapamiento(cancha.getId(), ocupacion.getId(),
                    request.fechaHoraInicio(), request.fechaHoraFin());
        }

        ocupacion.setCancha(cancha);
        ocupacion.setFechaHoraInicio(request.fechaHoraInicio());
        ocupacion.setFechaHoraFin(request.fechaHoraFin());
        aplicarDatos(entrenamiento, request);

        Entrenamiento guardado = repository.save(entrenamiento);
        if (cambioHorario) {
            notificarReprogramacion(guardado, vigentes);
        }
        return guardado;
    }

    /** Baja del turno: libera la ocupacion, cancela inscripciones y avisa. */
    @Transactional
    public Entrenamiento cancelar(Integer id) {
        verificarGestion();
        Entrenamiento entrenamiento = obtener(id);
        if (entrenamiento.getEstado() != EstadoEntrenamiento.PROGRAMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El entrenamiento ya esta cancelado");
        }

        List<InscripcionEntrenamiento> vigentes = inscripcionesVigentes(id);
        entrenamiento.setEstado(EstadoEntrenamiento.CANCELADO);
        entrenamiento.getOcupacion().setActiva(false);
        vigentes.forEach(inscripcion -> inscripcion.setEstado(EstadoInscripcion.CANCELADO));

        Entrenamiento guardado = repository.save(entrenamiento);
        notificarCancelacion(guardado, vigentes);
        return guardado;
    }

    /**
     * Inscribe al usuario autenticado. Solo un CLIENTE se inscribe a si mismo:
     * el backend ignora cualquier id de usuario que venga del cliente.
     */
    @Transactional
    public InscripcionEntrenamiento inscribir(Integer id) {
        Usuario actual = currentUser.get();
        if (!Rol.CLIENTE.equals(actual.getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un cliente puede inscribirse a un entrenamiento");
        }

        Entrenamiento entrenamiento = obtener(id);
        if (entrenamiento.getEstado() != EstadoEntrenamiento.PROGRAMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El entrenamiento esta cancelado");
        }
        if (entrenamiento.getOcupacion().getFechaHoraInicio().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El entrenamiento ya comenzo");
        }

        Optional<InscripcionEntrenamiento> existente = inscripcionRepository
                .findByEntrenamiento_IdAndUsuario_Id(id, actual.getId());
        if (existente.isPresent() && existente.get().getEstado() == EstadoInscripcion.INSCRITO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya estas inscripto en este entrenamiento");
        }

        long vigentes = inscripcionRepository.countByEntrenamiento_IdAndEstado(id,
                EstadoInscripcion.INSCRITO);
        if (vigentes >= entrenamiento.getCupoMaximo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No quedan cupos disponibles");
        }

        InscripcionEntrenamiento inscripcion = existente.orElseGet(() -> {
            InscripcionEntrenamiento nueva = new InscripcionEntrenamiento();
            nueva.setEntrenamiento(entrenamiento);
            nueva.setUsuario(actual);
            return nueva;
        });
        inscripcion.setEstado(EstadoInscripcion.INSCRITO);
        inscripcion.setFechaInscripcion(LocalDateTime.now());
        return inscripcionRepository.save(inscripcion);
    }

    /** Baja del cupo propio; conserva la fila para poder volver a inscribirse. */
    @Transactional
    public void cancelarInscripcion(Integer id) {
        Usuario actual = currentUser.get();
        InscripcionEntrenamiento inscripcion = inscripcionRepository
                .findByEntrenamiento_IdAndUsuario_Id(id, actual.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No estas inscripto en este entrenamiento"));
        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La inscripcion ya esta cancelada");
        }
        inscripcion.setEstado(EstadoInscripcion.CANCELADO);
        inscripcionRepository.save(inscripcion);
    }

    private Entrenamiento obtener(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el entrenamiento con id " + id));
    }

    private EntrenamientoResponse aResponse(Entrenamiento entrenamiento, Usuario actual) {
        long inscriptos = inscripcionRepository.countByEntrenamiento_IdAndEstado(
                entrenamiento.getId(), EstadoInscripcion.INSCRITO);
        boolean inscripto = inscripcionRepository
                .findByEntrenamiento_IdAndUsuario_Id(entrenamiento.getId(), actual.getId())
                .map(inscripcion -> inscripcion.getEstado() == EstadoInscripcion.INSCRITO)
                .orElse(false);
        return EntrenamientoResponse.toResponse(entrenamiento, inscriptos, inscripto);
    }

    private List<InscripcionEntrenamiento> inscripcionesVigentes(Integer id) {
        return inscripcionRepository.findByEntrenamiento_IdAndEstado(id, EstadoInscripcion.INSCRITO);
    }

    private void notificarCancelacion(Entrenamiento entrenamiento,
            List<InscripcionEntrenamiento> inscripciones) {
        for (EntrenamientoObserver observer : observers) {
            observer.alCancelar(entrenamiento, inscripciones);
        }
    }

    private void notificarReprogramacion(Entrenamiento entrenamiento,
            List<InscripcionEntrenamiento> inscripciones) {
        for (EntrenamientoObserver observer : observers) {
            observer.alReprogramar(entrenamiento, inscripciones);
        }
    }

    private void aplicarDatos(Entrenamiento entrenamiento, EntrenamientoRequest request) {
        entrenamiento.setTitulo(request.titulo());
        entrenamiento.setEntrenador(request.entrenador());
        entrenamiento.setDescripcion(request.descripcion());
        entrenamiento.setCupoMaximo(request.cupoMaximo());
        entrenamiento.setPrecio(request.precio() != null ? request.precio() : BigDecimal.ZERO);
    }

    private Cancha canchaActiva(Integer canchaId) {
        Cancha cancha = canchaRepository.findById(canchaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la cancha con id " + canchaId));
        if (!cancha.isActiva()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cancha " + cancha.getNombre() + " esta inactiva");
        }
        return cancha;
    }

    private void validarHorario(LocalDateTime inicio, LocalDateTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de fin debe ser posterior a la de inicio");
        }
    }

    private void verificarSolapamiento(Integer canchaId, Integer ocupacionPropiaId,
            LocalDateTime inicio, LocalDateTime fin) {
        if (ocupacionRepository.existeSolapamiento(canchaId, ocupacionPropiaId, inicio, fin)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cancha ya esta ocupada en ese horario");
        }
    }

    private void verificarGestion() {
        Rol rol = currentUser.get().getRol();
        if (rol != Rol.ADMIN && rol != Rol.RECEPCIONISTA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el admin o la recepcion gestionan entrenamientos");
        }
    }
}
