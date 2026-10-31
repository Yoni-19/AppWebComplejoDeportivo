package com.proyectosw.sreservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.model.Implemento;
import com.proyectosw.sreservas.repository.DetalleReservaRepository;
import com.proyectosw.sreservas.repository.ImplementoRepository;
import com.proyectosw.sreservas.repository.MovimientoImplementoRepository;

@ExtendWith(MockitoExtension.class)
class ImplementoServiceTests {

    @Mock
    private ImplementoRepository repository;
    @Mock
    private MovimientoImplementoRepository movimientoRepository;
    @Mock
    private DetalleReservaRepository detalleRepository;

    @InjectMocks
    private ImplementoService service;

    @Test
    void noSeRepiteElNombreDelImplemento() {
        Implemento datos = implemento("Pelota de futbol");
        when(repository.findByNombreIgnoreCase("Pelota de futbol"))
                .thenReturn(Optional.of(implemento("Pelota de futbol")));

        assertThatThrownBy(() -> service.crear(datos))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any(Implemento.class));
    }

    @Test
    void elCatalogoSeListaCompleto() {
        when(repository.findAll()).thenReturn(List.of(
                implemento("Pelota de futbol"), implemento("Red de voley")));

        assertThat(service.listarTodos()).hasSize(2);
    }

    /**
     * Un implemento que ya se presto no se borra: el historial de movimientos
     * quedaria apuntando a la nada. Se desactiva en vez de eliminarlo.
     */
    @Test
    void noSeBorraUnImplementoConMovimientos() {
        when(repository.existsById(7)).thenReturn(true);
        when(movimientoRepository.existsByImplemento_Id(7)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(7))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).deleteById(7);
    }

    /**
     * Un implemento pedido en una reserva pero todavia no entregado ya esta
     * referenciado por una linea del detalle: borrarlo romperia esa reserva.
     */
    @Test
    void noSeBorraUnImplementoPedidoEnUnaReserva() {
        when(repository.existsById(7)).thenReturn(true);
        when(movimientoRepository.existsByImplemento_Id(7)).thenReturn(false);
        when(detalleRepository.existsByImplemento_Id(7)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(7))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).deleteById(7);
    }

    @Test
    void sePuedeDesactivarUnImplementoEnVezDeBorrarlo() {
        Implemento existente = implemento("Pelota de futbol");
        existente.setId(7);
        existente.setActivo(true);
        when(repository.findById(7)).thenReturn(Optional.of(existente));
        when(repository.save(any(Implemento.class))).thenAnswer(inv -> inv.getArgument(0));

        Implemento datos = implemento("Pelota de futbol");
        datos.setActivo(false);
        Implemento actualizado = service.actualizar(7, datos);

        assertThat(actualizado.isActivo()).isFalse();
    }

    private Implemento implemento(String nombre) {
        Implemento implemento = new Implemento();
        implemento.setNombre(nombre);
        implemento.setValorReposicion(new BigDecimal("25.00"));
        implemento.setActivo(true);
        return implemento;
    }
}
