package com.proyectosw.sreservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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

import com.proyectosw.sreservas.dto.CargoRequest;
import com.proyectosw.sreservas.dto.DevolucionImplementoRequest;
import com.proyectosw.sreservas.dto.EntregaImplementoRequest;
import com.proyectosw.sreservas.dto.ImplementoReservaResponse;
import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.DetalleReserva;
import com.proyectosw.sreservas.model.Implemento;
import com.proyectosw.sreservas.model.MovimientoImplemento;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.EstadoCuenta;
import com.proyectosw.sreservas.model.enums.EstadoServicio;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.model.enums.TipoDetalle;
import com.proyectosw.sreservas.model.enums.TipoMovimientoImplemento;
import com.proyectosw.sreservas.repository.DetalleReservaRepository;
import com.proyectosw.sreservas.repository.ImplementoRepository;
import com.proyectosw.sreservas.repository.MovimientoImplementoRepository;
import com.proyectosw.sreservas.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class UtileriaServiceTests {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ImplementoRepository implementoRepository;
    @Mock
    private MovimientoImplementoRepository movimientoRepository;
    @Mock
    private DetalleReservaRepository detalleRepository;
    @Mock
    private CuentaService cuentaService;
    @Mock
    private CurrentUserService currentUser;
    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private UtileriaService service;

    @Test
    void elEstadoDeImplementosMuestraPedidosEntregadosYDevueltos() {
        Reserva reserva = reservaEnCurso();
        when(reservaService.buscarPorId(5)).thenReturn(Optional.of(reserva));
        when(detalleRepository.findByReserva_Id(5)).thenReturn(List.of(lineaCancha(), lineaImplemento(2)));
        MovimientoImplemento entrega = movimiento(TipoMovimientoImplemento.ENTREGA, 2);
        MovimientoImplemento devolucion = movimiento(TipoMovimientoImplemento.DEVOLUCION, 1);
        when(movimientoRepository.findByReserva_IdOrderByIdAsc(5)).thenReturn(List.of(entrega, devolucion));
        when(implementoRepository.findById(7)).thenReturn(Optional.of(implemento()));

        ImplementoReservaResponse estado = service.listar(5);

        assertThat(estado.lineas()).hasSize(1);
        ImplementoReservaResponse.LineaImplemento linea = estado.lineas().get(0);
        assertThat(linea.nombre()).isEqualTo("Pelota de futbol");
        assertThat(linea.pedidos()).isEqualTo(2);
        assertThat(linea.entregados()).isEqualTo(2);
        assertThat(linea.devueltos()).isEqualTo(1);
        assertThat(linea.pendientes()).isEqualTo(1);
        assertThat(linea.valorReposicion()).isEqualByComparingTo("25.00");
    }

    @Test
    void laEntregaRegistraElMovimientoYNoCobraNada() {
        Reserva reserva = reservaEnCurso();
        when(currentUser.get()).thenReturn(usuario(Rol.UTILERIA));
        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));
        when(detalleRepository.findByReserva_Id(5)).thenReturn(List.of(lineaCancha(), lineaImplemento(2)));
        when(movimientoRepository.findByReserva_IdOrderByIdAsc(5)).thenReturn(List.of());
        when(implementoRepository.findById(7)).thenReturn(Optional.of(implemento()));

        service.entregar(5, List.of(new EntregaImplementoRequest(7, 2)));

        verify(movimientoRepository).save(argThat(movimiento ->
                movimiento.getTipo() == TipoMovimientoImplemento.ENTREGA
                        && movimiento.getCantidad() == 2));
        verify(cuentaService, never()).recalcularTotal(any(Cuenta.class), any(BigDecimal.class));
    }

    @Test
    void noSePuedeEntregarMasDeLoPedido() {
        Reserva reserva = reservaEnCurso();
        when(currentUser.get()).thenReturn(usuario(Rol.UTILERIA));
        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));
        when(detalleRepository.findByReserva_Id(5)).thenReturn(List.of(lineaImplemento(2)));
        when(movimientoRepository.findByReserva_IdOrderByIdAsc(5)).thenReturn(List.of());
        when(implementoRepository.findById(7)).thenReturn(Optional.of(implemento()));

        assertThatThrownBy(() -> service.entregar(5, List.of(new EntregaImplementoRequest(7, 3))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(movimientoRepository, never()).save(any(MovimientoImplemento.class));
    }

    @Test
    void noSeEntreganImplementosDeUnaReservaConfirmada() {
        Reserva confirmada = reservaEnCurso();
        confirmada.setEstado(EstadoServicio.CONFIRMADA);
        when(currentUser.get()).thenReturn(usuario(Rol.UTILERIA));
        when(reservaRepository.findById(5)).thenReturn(Optional.of(confirmada));

        assertThatThrownBy(() -> service.entregar(5, List.of(new EntregaImplementoRequest(7, 1))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void elClienteNoPuedeRegistrarMovimientosDeUtileria() {
        when(currentUser.get()).thenReturn(usuario(Rol.CLIENTE));

        assertThatThrownBy(() -> service.entregar(5, List.of(new EntregaImplementoRequest(7, 1))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(reservaRepository, never()).findById(any());
    }

    @Test
    void laDevolucionCobraElDanioYLaPerdidaSobreLaReserva() {
        Reserva reserva = reservaEnCurso();
        Cuenta cuenta = cuentaDe(new BigDecimal("150.00"));
        reserva.setCuenta(cuenta);
        when(currentUser.get()).thenReturn(usuario(Rol.UTILERIA));
        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));
        when(detalleRepository.findByReserva_Id(5))
                .thenAnswer(inv -> List.copyOf(reserva.getDetalles()));
        reserva.getDetalles().add(lineaCancha());
        reserva.getDetalles().add(lineaImplemento(2));
        when(movimientoRepository.findByReserva_IdOrderByIdAsc(5))
                .thenReturn(List.of(movimiento(TipoMovimientoImplemento.ENTREGA, 2)));
        when(implementoRepository.findById(7)).thenReturn(Optional.of(implemento()));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva devuelta = service.devolver(5, List.of(
                new DevolucionImplementoRequest(7, 1, 1, null, null)));

        // Vuelve 1 (danada) y falta 1 (perdida): 25 + 25 sobre los 150 de la cancha.
        assertThat(devuelta.getMontoTotal()).isEqualByComparingTo("200.00");
        verify(cuentaService).recalcularTotal(eq(cuenta), argThat(total ->
                total.compareTo(new BigDecimal("200.00")) == 0));
        assertThat(devuelta.getDetalles())
                .extracting(DetalleReserva::getTipo)
                .contains(TipoDetalle.DANIO, TipoDetalle.PERDIDA);
    }

    @Test
    void lasLineasRepetidasDelMismoImplementoSeAgrupan() {
        Reserva reserva = reservaEnCurso();
        Cuenta cuenta = cuentaDe(new BigDecimal("150.00"));
        reserva.setCuenta(cuenta);
        when(currentUser.get()).thenReturn(usuario(Rol.UTILERIA));
        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));
        when(detalleRepository.findByReserva_Id(5))
                .thenAnswer(inv -> List.copyOf(reserva.getDetalles()));
        reserva.getDetalles().add(lineaCancha());
        reserva.getDetalles().add(lineaImplemento(2));
        when(movimientoRepository.findByReserva_IdOrderByIdAsc(5))
                .thenReturn(List.of(movimiento(TipoMovimientoImplemento.ENTREGA, 2)));
        when(implementoRepository.findById(7)).thenReturn(Optional.of(implemento()));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva devuelta = service.devolver(5, List.of(
                new DevolucionImplementoRequest(7, 1, 0, null, null),
                new DevolucionImplementoRequest(7, 1, 1, null, null)));

        // Vuelven las 2 (una danada): no falta ninguna unidad, asi que no hay PERDIDA.
        verify(movimientoRepository).save(argThat(movimiento ->
                movimiento.getTipo() == TipoMovimientoImplemento.DEVOLUCION
                        && movimiento.getCantidad() == 2));
        assertThat(devuelta.getDetalles())
                .extracting(DetalleReserva::getTipo)
                .contains(TipoDetalle.DANIO)
                .doesNotContain(TipoDetalle.PERDIDA);
        assertThat(devuelta.getMontoTotal()).isEqualByComparingTo("175.00");
    }

    @Test
    void noSePuedenDevolverMasUnidadesQueLasEntregadas() {
        Reserva reserva = reservaEnCurso();
        when(currentUser.get()).thenReturn(usuario(Rol.UTILERIA));
        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));
        when(movimientoRepository.findByReserva_IdOrderByIdAsc(5))
                .thenReturn(List.of(movimiento(TipoMovimientoImplemento.ENTREGA, 1)));
        when(implementoRepository.findById(7)).thenReturn(Optional.of(implemento()));

        assertThatThrownBy(() -> service.devolver(5, List.of(
                new DevolucionImplementoRequest(7, 2, 0, null, null))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void lasDanadasNoPuedenSuperarLasDevueltas() {
        Reserva reserva = reservaEnCurso();
        when(currentUser.get()).thenReturn(usuario(Rol.UTILERIA));
        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));
        when(movimientoRepository.findByReserva_IdOrderByIdAsc(5))
                .thenReturn(List.of(movimiento(TipoMovimientoImplemento.ENTREGA, 2)));
        when(implementoRepository.findById(7)).thenReturn(Optional.of(implemento()));

        assertThatThrownBy(() -> service.devolver(5, List.of(
                new DevolucionImplementoRequest(7, 1, 2, null, null))))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unCargoDeServicioSeSumaAlTotalDeLaReserva() {
        Reserva reserva = reservaEnCurso();
        Cuenta cuenta = cuentaDe(new BigDecimal("150.00"));
        reserva.setCuenta(cuenta);
        when(currentUser.get()).thenReturn(usuario(Rol.UTILERIA));
        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));
        when(detalleRepository.findByReserva_Id(5))
                .thenAnswer(inv -> List.copyOf(reserva.getDetalles()));
        reserva.getDetalles().add(lineaCancha());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva conCargo = service.agregarCargo(5,
                new CargoRequest(TipoDetalle.SERVICIO, "Iluminacion", 2, new BigDecimal("10.00")));

        assertThat(conCargo.getMontoTotal()).isEqualByComparingTo("170.00");
        verify(cuentaService).recalcularTotal(eq(cuenta), argThat(total ->
                total.compareTo(new BigDecimal("170.00")) == 0));
    }

    private Reserva reservaEnCurso() {
        Reserva reserva = new Reserva();
        reserva.setId(5);
        reserva.setEstado(EstadoServicio.EN_CURSO);
        reserva.setMontoTotal(new BigDecimal("150.00"));
        return reserva;
    }

    private DetalleReserva lineaCancha() {
        DetalleReserva linea = new DetalleReserva();
        linea.setTipo(TipoDetalle.CANCHA);
        linea.setDescripcion("Cancha 1");
        linea.setCantidad(2);
        linea.setPrecioUnitario(new BigDecimal("75.00"));
        linea.setSubtotal(new BigDecimal("150.00"));
        return linea;
    }

    private DetalleReserva lineaImplemento(int cantidad) {
        DetalleReserva linea = new DetalleReserva();
        linea.setTipo(TipoDetalle.IMPLEMENTO);
        linea.setImplemento(implemento());
        linea.setDescripcion("Pelota de futbol");
        linea.setCantidad(cantidad);
        linea.setPrecioUnitario(BigDecimal.ZERO);
        linea.setSubtotal(BigDecimal.ZERO);
        return linea;
    }

    private MovimientoImplemento movimiento(TipoMovimientoImplemento tipo, int cantidad) {
        MovimientoImplemento movimiento = new MovimientoImplemento();
        movimiento.setReserva(reservaEnCurso());
        movimiento.setImplemento(implemento());
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setFecha(LocalDateTime.now());
        return movimiento;
    }

    private Implemento implemento() {
        Implemento implemento = new Implemento();
        implemento.setId(7);
        implemento.setNombre("Pelota de futbol");
        implemento.setValorReposicion(new BigDecimal("25.00"));
        implemento.setActivo(true);
        return implemento;
    }

    private Cuenta cuentaDe(BigDecimal total) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(1);
        cuenta.setTotal(total);
        cuenta.setSaldo(total);
        cuenta.setEstado(EstadoCuenta.ABIERTA);
        return cuenta;
    }

    private Usuario usuario(Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(9);
        usuario.setNombres("Nombre");
        usuario.setApellidos("Apellido");
        usuario.setCorreo("utileria@test.com");
        usuario.setClave("hash");
        usuario.setRol(rol);
        return usuario;
    }
}
