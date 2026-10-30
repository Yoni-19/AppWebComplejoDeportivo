package com.proyectosw.sreservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.dto.ReservaRequest;
import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.Ocupacion;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.EstadoCuenta;
import com.proyectosw.sreservas.model.enums.EstadoServicio;
import com.proyectosw.sreservas.model.enums.MetodoPago;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.model.enums.TipoOcupacion;
import com.proyectosw.sreservas.repository.CanchaRepository;
import com.proyectosw.sreservas.repository.CuentaRepository;
import com.proyectosw.sreservas.repository.DetalleReservaRepository;
import com.proyectosw.sreservas.repository.OcupacionRepository;
import com.proyectosw.sreservas.repository.PagoRepository;
import com.proyectosw.sreservas.repository.ReservaRepository;
import com.proyectosw.sreservas.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTests {

    @Mock
    private ReservaRepository repository;
    @Mock
    private OcupacionRepository ocupacionRepository;
    @Mock
    private CanchaRepository canchaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CurrentUserService currentUser;
    @Mock
    private DetalleReservaRepository detalleRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private ReservaService service;

    @Test
    void elClienteSoloListaSusPropiasReservas() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        when(repository.findByUsuario_Id(2)).thenReturn(List.of(reservaDe(2)));

        List<Reserva> reservas = service.listarTodos();

        assertThat(reservas).hasSize(1);
        verify(repository, never()).findAll();
    }

    @Test
    void elRecepcionistaListaTodasLasReservas() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.RECEPCIONISTA));
        when(repository.findAll()).thenReturn(List.of(reservaDe(2), reservaDe(3)));

        assertThat(service.listarTodos()).hasSize(2);
        verify(repository, never()).findByUsuario_Id(anyInt());
    }

    @Test
    void elClienteNoPuedeVerUnaReservaAjena() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        when(repository.findById(5)).thenReturn(Optional.of(reservaDe(3)));

        assertThatThrownBy(() -> service.buscarPorId(5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void elClienteNoPuedeReservarAParaOtroUsuario() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        when(canchaRepository.findById(1)).thenReturn(Optional.of(canchaActiva()));
        when(repository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaService.crear(any(Reserva.class), any(BigDecimal.class)))
                .thenAnswer(inv -> cuentaDe(inv.getArgument(1)));

        Reserva creada = service.crear(request(1, 3));

        assertThat(creada.getUsuario().getId()).isEqualTo(2);
        assertThat(creada.getEstado()).isEqualTo(EstadoServicio.CONFIRMADA);
        assertThat(creada.getMontoTotal()).isEqualByComparingTo("150.00");
        assertThat(creada.getOcupacion().getTipo()).isEqualTo(TipoOcupacion.RESERVA);
        assertThat(creada.getOcupacion().isActiva()).isTrue();
        assertThat(creada.getDetalles()).hasSize(1);
        assertThat(creada.getDetalles().get(0).getCantidad()).isEqualTo(2);
        assertThat(creada.getCuenta()).isNotNull();
        verify(cuentaService).registrarPago(any(Cuenta.class), any());
    }

    @Test
    void laReservaSinMetodoDePagoDelAnticipoFalla() {
        assertThatThrownBy(() -> service.crear(requestSinMetodo(1)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repository, never()).save(any(Reserva.class));
    }

    @Test
    void elClientePuedeReprogramarSuPropiaReserva() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        when(repository.findById(5)).thenReturn(Optional.of(reservaDe(2)));
        when(repository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detalleRepository.findFirstByReserva_IdAndTipo(anyInt(), any()))
                .thenReturn(Optional.empty());

        Reserva reprogramada = service.actualizar(5, request(1, null));

        assertThat(reprogramada.getOcupacion().getFechaHoraInicio())
                .isEqualTo(LocalDateTime.of(2026, 12, 1, 10, 0));
        assertThat(reprogramada.getMontoTotal()).isEqualByComparingTo("150.00");
        assertThat(reprogramada.getDetalles()).hasSize(1);
    }

    @Test
    void elClienteNoPuedeReprogramarUnaReservaAjena() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        when(repository.findById(5)).thenReturn(Optional.of(reservaDe(3)));

        assertThatThrownBy(() -> service.actualizar(5, request(1, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(repository, never()).save(any(Reserva.class));
    }

    @Test
    void elClienteNoPuedeEliminarUnaReservaAjena() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        when(repository.findById(5)).thenReturn(Optional.of(reservaDe(3)));

        assertThatThrownBy(() -> service.eliminar(5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(repository, never()).delete(any(Reserva.class));
    }

    @Test
    void cancelarMarcaLaReservaYLiberaElTurnoSinBorrarNada() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        Reserva reserva = reservaDe(2);
        Cuenta cuenta = cuentaDe(new BigDecimal("75.00"));
        reserva.setCuenta(cuenta);
        when(repository.findById(5)).thenReturn(Optional.of(reserva));
        when(repository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva cancelada = service.cancelar(5);

        assertThat(cancelada.getEstado()).isEqualTo(EstadoServicio.CANCELADA);
        assertThat(cancelada.getOcupacion().isActiva()).isFalse();
        verify(cuentaService).anular(cuenta);
        verify(repository, never()).delete(any(Reserva.class));
        verify(ocupacionRepository, never()).delete(any(Ocupacion.class));
    }

    @Test
    void noSePuedeCancelarUnaReservaCompletada() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.ADMIN));
        Reserva completada = reservaDe(2);
        completada.setEstado(EstadoServicio.COMPLETADA);
        when(repository.findById(5)).thenReturn(Optional.of(completada));

        assertThatThrownBy(() -> service.cancelar(5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(cuentaService, never()).anular(any(Cuenta.class));
    }

    @Test
    void noSePuedeCancelarUnaReservaYaCancelada() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.ADMIN));
        Reserva cancelada = reservaDe(2);
        cancelada.setEstado(EstadoServicio.CANCELADA);
        when(repository.findById(5)).thenReturn(Optional.of(cancelada));

        assertThatThrownBy(() -> service.cancelar(5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void elClienteNoPuedeCancelarUnaReservaAjena() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        when(repository.findById(5)).thenReturn(Optional.of(reservaDe(3)));

        assertThatThrownBy(() -> service.cancelar(5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void noSePuedeReprogramarUnaReservaCancelada() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.ADMIN));
        Reserva cancelada = reservaDe(2);
        cancelada.setEstado(EstadoServicio.CANCELADA);
        when(repository.findById(5)).thenReturn(Optional.of(cancelada));

        assertThatThrownBy(() -> service.actualizar(5, request(1, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any(Reserva.class));
    }

    @Test
    void elAdminPuedeBorrarFisicamenteUnaReservaSinPagos() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.ADMIN));
        Reserva reserva = reservaDe(2);
        when(repository.findById(5)).thenReturn(Optional.of(reserva));

        service.eliminar(5);

        verify(repository).delete(reserva);
        verify(ocupacionRepository).delete(any(Ocupacion.class));
    }

    @Test
    void noSePuedeReprogramarUnaReservaCompletada() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.ADMIN));
        Reserva completada = reservaDe(2);
        completada.setEstado(EstadoServicio.COMPLETADA);
        when(repository.findById(5)).thenReturn(Optional.of(completada));

        assertThatThrownBy(() -> service.actualizar(5, request(1, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void noSePuedeReprogramarUnaReservaEnCurso() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.RECEPCIONISTA));
        Reserva enCurso = reservaVigente();
        enCurso.setEstado(EstadoServicio.EN_CURSO);
        when(repository.findById(5)).thenReturn(Optional.of(enCurso));

        assertThatThrownBy(() -> service.actualizar(5, request(1, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any(Reserva.class));
    }

    @Test
    void elCheckInPoneLaReservaEnCurso() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.RECEPCIONISTA));
        when(repository.findById(5)).thenReturn(Optional.of(reservaVigente()));
        when(repository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva enCurso = service.registrarCheckIn(5);

        assertThat(enCurso.getEstado()).isEqualTo(EstadoServicio.EN_CURSO);
    }

    @Test
    void noSePuedeHacerCheckInAntesDelHorarioDelTurno() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.RECEPCIONISTA));
        when(repository.findById(5)).thenReturn(Optional.of(reservaDe(2)));

        assertThatThrownBy(() -> service.registrarCheckIn(5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any(Reserva.class));
    }

    @Test
    void elClienteNoPuedeRegistrarCheckInNiCheckOut() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));

        assertThatThrownBy(() -> service.registrarCheckIn(5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> service.registrarCheckOut(5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(repository, never()).findById(anyInt());
    }

    @Test
    void elCheckOutCompletaLaReservaEnCurso() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.RECEPCIONISTA));
        Reserva enCurso = reservaVigente();
        enCurso.setEstado(EstadoServicio.EN_CURSO);
        when(repository.findById(5)).thenReturn(Optional.of(enCurso));
        when(repository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva completada = service.registrarCheckOut(5);

        assertThat(completada.getEstado()).isEqualTo(EstadoServicio.COMPLETADA);
    }

    @Test
    void noSePuedeHacerCheckOutSinCheckIn() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.RECEPCIONISTA));
        when(repository.findById(5)).thenReturn(Optional.of(reservaVigente()));

        assertThatThrownBy(() -> service.registrarCheckOut(5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any(Reserva.class));
    }

    @Test
    void losTurnosVencidosSinCheckInQuedanNoAsistio() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.RECEPCIONISTA));
        Reserva vencida = reservaDe(2);
        Ocupacion ocupacion = vencida.getOcupacion();
        ocupacion.setActiva(true);
        ocupacion.setFechaHoraInicio(LocalDateTime.now().minusHours(3));
        ocupacion.setFechaHoraFin(LocalDateTime.now().minusHours(2));
        when(repository.findByEstado(EstadoServicio.CONFIRMADA)).thenReturn(List.of(vencida));
        when(repository.findAll()).thenReturn(List.of(vencida));

        service.listarTodos();

        assertThat(vencida.getEstado()).isEqualTo(EstadoServicio.NO_ASISTIO);
        assertThat(ocupacion.isActiva()).isFalse();
    }

    private Usuario usuario(Integer id, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombres("Nombre" + id);
        usuario.setApellidos("Apellido" + id);
        usuario.setCorreo("usuario" + id + "@test.com");
        usuario.setClave("hash");
        usuario.setRol(rol);
        return usuario;
    }

    private Reserva reservaDe(Integer usuarioId) {
        Reserva reserva = new Reserva();
        reserva.setId(5);
        reserva.setUsuario(usuario(usuarioId, Rol.CLIENTE));

        Ocupacion ocupacion = new Ocupacion();
        ocupacion.setId(50);
        ocupacion.setCancha(canchaActiva());
        ocupacion.setFechaHoraInicio(LocalDateTime.of(2026, 12, 1, 10, 0));
        ocupacion.setFechaHoraFin(LocalDateTime.of(2026, 12, 1, 11, 0));
        reserva.setOcupacion(ocupacion);
        reserva.setEstado(EstadoServicio.CONFIRMADA);
        reserva.setMontoTotal(new BigDecimal("75.00"));
        return reserva;
    }

    /** Reserva en pleno turno: el check-in se permite desde una hora antes. */
    private Reserva reservaVigente() {
        Reserva reserva = reservaDe(2);
        LocalDateTime ahora = LocalDateTime.now();
        reserva.getOcupacion().setFechaHoraInicio(ahora.minusMinutes(10));
        reserva.getOcupacion().setFechaHoraFin(ahora.plusMinutes(50));
        return reserva;
    }

    private Cuenta cuentaDe(BigDecimal total) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(1);
        cuenta.setTotal(total);
        cuenta.setSaldo(total);
        cuenta.setEstado(EstadoCuenta.ABIERTA);
        return cuenta;
    }

    private ReservaRequest request(Integer canchaId, Integer usuarioId) {
        return new ReservaRequest(
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 1, 11, 30),
                canchaId,
                usuarioId,
                MetodoPago.EFECTIVO);
    }

    private ReservaRequest requestSinMetodo(Integer canchaId) {
        return new ReservaRequest(
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 1, 11, 30),
                canchaId,
                null,
                null);
    }

    private Cancha canchaActiva() {
        Cancha cancha = new Cancha();
        cancha.setId(1);
        cancha.setNombre("Cancha 1");
        cancha.setActiva(true);
        cancha.setPrecioPorHora(new BigDecimal("75.00"));
        return cancha;
    }
}
