package com.proyectosw.sreservas.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.EstadoCuenta;
import com.proyectosw.sreservas.model.enums.TipoPago;
import com.proyectosw.sreservas.repository.CuentaRepository;
import com.proyectosw.sreservas.repository.PagoRepository;

import jakarta.transaction.Transactional;

/**
 * Mantiene la cabecera contable de la reserva: total, saldo y estado.
 *
 * Toda plata que entra o sale pasa por aca, asi el saldo nunca queda
 * desincronizado con los pagos guardados.
 */
@Service
public class CuentaService {

    private final CuentaRepository cuentaRepository;
    private final PagoRepository pagoRepository;

    public CuentaService(final CuentaRepository cuentaRepository, final PagoRepository pagoRepository) {
        this.cuentaRepository = cuentaRepository;
        this.pagoRepository = pagoRepository;
    }

    @Transactional
    public Cuenta crear(Reserva reserva, BigDecimal total) {
        Cuenta cuenta = new Cuenta();
        cuenta.setReserva(reserva);
        cuenta.setTotal(total);
        cuenta.setSaldo(total);
        cuenta.setEstado(EstadoCuenta.ABIERTA);
        return cuentaRepository.save(cuenta);
    }

    @Transactional
    public void registrarPago(Cuenta cuenta, Pago pago) {
        pago.setCuenta(cuenta);
        pagoRepository.save(pago);
        cuenta.getPagos().add(pago);
        aplicarMovimiento(cuenta, pago);
    }

    /** Recalcula el saldo desde cero usando los pagos guardados. */
    @Transactional
    public void recalcularSaldo(Cuenta cuenta) {
        BigDecimal saldo = cuenta.getTotal();
        for (Pago pago : pagoRepository.findByCuenta_Id(cuenta.getId())) {
            saldo = pago.getTipo() == TipoPago.REEMBOLSO
                    ? saldo.add(pago.getMonto())
                    : saldo.subtract(pago.getMonto());
        }
        cuenta.setSaldo(saldo);
        actualizarEstado(cuenta);
        cuentaRepository.save(cuenta);
    }

    @Transactional
    public void recalcularTotal(Cuenta cuenta, BigDecimal total) {
        cuenta.setTotal(total);
        recalcularSaldo(cuenta);
    }

    /**
     * Anula la cuenta de un servicio cancelado. No se reclama nada mas, asi que
     * el saldo queda en cero; los pagos ya registrados se conservan como historial.
     */
    @Transactional
    public void anular(Cuenta cuenta) {
        cuenta.setEstado(EstadoCuenta.ANULADA);
        cuenta.setSaldo(BigDecimal.ZERO);
        cuentaRepository.save(cuenta);
    }

    private void aplicarMovimiento(Cuenta cuenta, Pago pago) {
        BigDecimal saldo = pago.getTipo() == TipoPago.REEMBOLSO
                ? cuenta.getSaldo().add(pago.getMonto())
                : cuenta.getSaldo().subtract(pago.getMonto());
        cuenta.setSaldo(saldo);
        actualizarEstado(cuenta);
        cuentaRepository.save(cuenta);
    }

    private void actualizarEstado(Cuenta cuenta) {
        if (cuenta.getEstado() == EstadoCuenta.ANULADA) {
            return;
        }
        cuenta.setEstado(cuenta.getSaldo().signum() <= 0
                ? EstadoCuenta.CERRADA
                : EstadoCuenta.ABIERTA);
    }
}
