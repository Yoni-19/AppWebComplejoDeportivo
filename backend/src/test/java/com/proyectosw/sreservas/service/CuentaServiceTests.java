package com.proyectosw.sreservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.EstadoCuenta;
import com.proyectosw.sreservas.model.enums.MetodoPago;
import com.proyectosw.sreservas.model.enums.TipoPago;
import com.proyectosw.sreservas.repository.CuentaRepository;
import com.proyectosw.sreservas.repository.PagoRepository;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTests {

    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private CuentaService service;

    @Test
    void laCuentaNaceAbiertaConElTotalComoSaldo() {
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        Cuenta cuenta = service.crear(new Reserva(), new BigDecimal("140.00"));

        assertThat(cuenta.getTotal()).isEqualByComparingTo("140.00");
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("140.00");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ABIERTA);
    }

    @Test
    void elAnticipoBajaElSaldoYDejaLaCuentaAbierta() {
        Cuenta cuenta = cuenta("140.00");

        service.registrarPago(cuenta, pago(TipoPago.ANTICIPO, "70.00"));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("70.00");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ABIERTA);
        assertThat(cuenta.getPagos()).hasSize(1);
    }

    @Test
    void elSaldoFinalCierraLaCuenta() {
        Cuenta cuenta = cuenta("140.00");

        service.registrarPago(cuenta, pago(TipoPago.ANTICIPO, "70.00"));
        service.registrarPago(cuenta, pago(TipoPago.SALDO, "70.00"));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.CERRADA);
    }

    @Test
    void elReembolsoVuelveASubirElSaldo() {
        Cuenta cuenta = cuenta("140.00");

        service.registrarPago(cuenta, pago(TipoPago.ANTICIPO, "70.00"));
        service.registrarPago(cuenta, pago(TipoPago.REEMBOLSO, "70.00"));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("140.00");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ABIERTA);
    }

    private Cuenta cuenta(String total) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(1);
        cuenta.setTotal(new BigDecimal(total));
        cuenta.setSaldo(new BigDecimal(total));
        cuenta.setEstado(EstadoCuenta.ABIERTA);
        return cuenta;
    }

    private Pago pago(TipoPago tipo, String monto) {
        Pago pago = new Pago();
        pago.setTipo(tipo);
        pago.setMonto(new BigDecimal(monto));
        pago.setMetodoPago(MetodoPago.EFECTIVO);
        return pago;
    }
}
