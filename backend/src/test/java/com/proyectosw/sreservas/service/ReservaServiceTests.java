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

import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.EstadoReserva;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.repository.CanchaRepository;
import com.proyectosw.sreservas.repository.ReservaRepository;
import com.proyectosw.sreservas.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTests {

    @Mock
    private ReservaRepository repository;
    @Mock
    private CanchaRepository canchaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CurrentUserService currentUser;

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

        Reserva creada = service.crear(reservaPara(1, 3));

        assertThat(creada.getUsuario().getId()).isEqualTo(2);
        assertThat(creada.getEstadoReserva()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(creada.getMontoTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void elClientePuedeReprogramarSuPropiaReserva() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        when(repository.findById(5)).thenReturn(Optional.of(reservaDe(2)));
        when(canchaRepository.findById(1)).thenReturn(Optional.of(canchaActiva()));
        when(repository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva reprogramada = service.actualizar(5, reservaPara(1, null));

        assertThat(reprogramada.getFechaHoraInicio()).isEqualTo(LocalDateTime.of(2026, 12, 1, 10, 0));
        assertThat(reprogramada.getMontoTotal()).isEqualByComparingTo("150.00");
    }

    @Test
    void elClienteNoPuedeReprogramarUnaReservaAjena() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        when(repository.findById(5)).thenReturn(Optional.of(reservaDe(3)));

        assertThatThrownBy(() -> service.actualizar(5, reservaPara(1, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(repository, never()).save(any(Reserva.class));
    }

    @Test
    void elClienteNoPuedeCancelarUnaReservaAjena() {
        when(currentUser.get()).thenReturn(usuario(2, Rol.CLIENTE));
        when(repository.findById(5)).thenReturn(Optional.of(reservaDe(3)));

        assertThatThrownBy(() -> service.eliminar(5))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(repository, never()).delete(any(Reserva.class));
    }

    @Test
    void noSePuedeReprogramarUnaReservaCompletada() {
        when(currentUser.get()).thenReturn(usuario(9, Rol.ADMIN));
        Reserva completada = reservaDe(2);
        completada.setEstadoReserva(EstadoReserva.COMPLETADA);
        when(repository.findById(5)).thenReturn(Optional.of(completada));

        assertThatThrownBy(() -> service.actualizar(5, reservaPara(1, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
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
        reserva.setCancha(canchaActiva());
        reserva.setFechaHoraInicio(LocalDateTime.of(2026, 12, 1, 10, 0));
        reserva.setFechaHoraFin(LocalDateTime.of(2026, 12, 1, 11, 0));
        return reserva;
    }

    private Reserva reservaPara(Integer canchaId, Integer usuarioId) {
        Cancha cancha = new Cancha();
        cancha.setId(canchaId);
        Reserva reserva = new Reserva();
        reserva.setCancha(cancha);
        reserva.setUsuario(usuario(usuarioId, Rol.CLIENTE));
        reserva.setFechaHoraInicio(LocalDateTime.of(2026, 12, 1, 10, 0));
        reserva.setFechaHoraFin(LocalDateTime.of(2026, 12, 1, 11, 30));
        return reserva;
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
