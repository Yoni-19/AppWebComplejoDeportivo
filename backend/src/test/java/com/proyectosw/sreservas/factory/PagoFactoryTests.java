package com.proyectosw.sreservas.factory;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.enums.EstadoPago;
import com.proyectosw.sreservas.model.enums.MetodoPago;
import com.proyectosw.sreservas.model.enums.TipoPago;

class PagoFactoryTests {

    @Test
    void elAnticipoEsLaMitadDelTotal() {
        Pago pago = PagoFactory.anticipo(cuenta("140.00", "140.00"), MetodoPago.EFECTIVO);

        assertThat(pago.getTipo()).isEqualTo(TipoPago.ANTICIPO);
        assertThat(pago.getMonto()).isEqualByComparingTo("70.00");
        assertThat(pago.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(pago.getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
    }

    @Test
    void elAnticipoRedondeaALaMitadExacta() {
        Pago pago = PagoFactory.anticipo(cuenta("90.00", "90.00"), MetodoPago.TARJETA);

        assertThat(pago.getMonto()).isEqualByComparingTo("45.00");
    }

    @Test
    void elSaldoTomaLoQueFaltaDeLaCuenta() {
        Pago pago = PagoFactory.saldo(cuenta("140.00", "70.00"), MetodoPago.TRANSFERENCIA);

        assertThat(pago.getTipo()).isEqualTo(TipoPago.SALDO);
        assertThat(pago.getMonto()).isEqualByComparingTo("70.00");
    }

    @Test
    void elReembolsoUsaElMontoIndicado() {
        Pago pago = PagoFactory.reembolso(cuenta("140.00", "70.00"), MetodoPago.EFECTIVO, new BigDecimal("35.00"));

        assertThat(pago.getTipo()).isEqualTo(TipoPago.REEMBOLSO);
        assertThat(pago.getMonto()).isEqualByComparingTo("35.00");
    }

    private Cuenta cuenta(String total, String saldo) {
        Cuenta cuenta = new Cuenta();
        cuenta.setTotal(new BigDecimal(total));
        cuenta.setSaldo(new BigDecimal(saldo));
        return cuenta;
    }
}
