package com.proyectosw.sreservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.dto.PagoRequest;
import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.EstadoCuenta;
import com.proyectosw.sreservas.model.enums.MetodoPago;
import com.proyectosw.sreservas.model.enums.TipoPago;
import com.proyectosw.sreservas.repository.CuentaRepository;
import com.proyectosw.sreservas.repository.PagoRepository;
import com.proyectosw.sreservas.repository.ReservaRepository;

@ExtendWith(MockitoExtension.class)
class PagoServiceTests {

    @Mock
    private PagoRepository repository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private PagoService service;

    @Test
    void unaCuentaAnuladaNoAdmiteCobros() {
        prepararCuentaAnulada();

        assertThatThrownBy(() -> service.crear(
                new PagoRequest(TipoPago.SALDO, MetodoPago.EFECTIVO, 5, null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(cuentaService, never()).registrarPago(any(Cuenta.class), any(Pago.class));
    }

    @Test
    void unaCuentaAnuladaSiAdmiteElReembolsoDeLoPagado() {
        Cuenta cuenta = prepararCuentaAnulada();

        Pago reembolso = service.crear(new PagoRequest(TipoPago.REEMBOLSO, MetodoPago.EFECTIVO, 5,
                new BigDecimal("70.00")));

        assertThat(reembolso.getTipo()).isEqualTo(TipoPago.REEMBOLSO);
        assertThat(reembolso.getMonto()).isEqualByComparingTo("70.00");
        verify(cuentaService).registrarPago(cuenta, reembolso);
    }

    private Cuenta prepararCuentaAnulada() {
        Reserva reserva = new Reserva();
        reserva.setId(5);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(1);
        cuenta.setReserva(reserva);
        cuenta.setTotal(new BigDecimal("140.00"));
        cuenta.setSaldo(BigDecimal.ZERO);
        cuenta.setEstado(EstadoCuenta.ANULADA);

        when(reservaRepository.findById(5)).thenReturn(Optional.of(reserva));
        when(cuentaRepository.findByReserva_Id(5)).thenReturn(Optional.of(cuenta));
        return cuenta;
    }
}
