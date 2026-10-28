package com.proyectosw.sreservas.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.dto.PagoRequest;
import com.proyectosw.sreservas.factory.PagoFactory;
import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.EstadoCuenta;
import com.proyectosw.sreservas.model.enums.TipoPago;
import com.proyectosw.sreservas.repository.CuentaRepository;
import com.proyectosw.sreservas.repository.PagoRepository;
import com.proyectosw.sreservas.repository.ReservaRepository;

import jakarta.transaction.Transactional;

/**
 * Registra movimientos de plata sobre la cuenta de una reserva.
 *
 * El cliente no elige el monto: el anticipo es la mitad del total, el saldo es
 * lo que falta y solo el reembolso recibe un importe explicito.
 */
@Service
public class PagoService {
    private final PagoRepository repository;
    private final ReservaRepository reservaRepository;
    private final CuentaRepository cuentaRepository;
    private final CuentaService cuentaService;

    public PagoService(final PagoRepository repository, final ReservaRepository reservaRepository,
            final CuentaRepository cuentaRepository, final CuentaService cuentaService) {
        this.repository = repository;
        this.reservaRepository = reservaRepository;
        this.cuentaRepository = cuentaRepository;
        this.cuentaService = cuentaService;
    }

    public List<Pago> listarTodos() {
        return repository.findAll();
    }

    public Optional<Pago> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public Pago crear(PagoRequest request) {
        Reserva reserva = reservaRepository.findById(request.reservaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la reserva con id " + request.reservaId()));
        Cuenta cuenta = cuentaRepository.findByReserva_Id(reserva.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La reserva " + reserva.getId() + " no tiene cuenta"));

        if (cuenta.getEstado() == EstadoCuenta.ANULADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta de la reserva esta anulada");
        }

        Pago pago = switch (request.tipo()) {
            case ANTICIPO -> crearAnticipo(cuenta, request);
            case SALDO -> crearSaldo(cuenta, request);
            case REEMBOLSO -> crearReembolso(cuenta, request);
        };

        cuentaService.registrarPago(cuenta, pago);
        return pago;
    }

    @Transactional
    public Pago actualizar(Integer id, PagoRequest request) {
        Pago existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el pago con id " + id));
        // El tipo y el monto no se editan: si hay que corregir, se elimina y se vuelve a registrar.
        if (request.metodoPago() != null) {
            existente.setMetodoPago(request.metodoPago());
        }
        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Integer id) {
        Pago existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el pago con id " + id));
        Cuenta cuenta = existente.getCuenta();
        repository.delete(existente);
        repository.flush();
        cuentaService.recalcularSaldo(cuenta);
    }

    private Pago crearAnticipo(Cuenta cuenta, PagoRequest request) {
        if (repository.existsByCuenta_IdAndTipo(cuenta.getId(), TipoPago.ANTICIPO)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La reserva ya tiene un anticipo registrado");
        }
        return PagoFactory.anticipo(cuenta, request.metodoPago());
    }

    private Pago crearSaldo(Cuenta cuenta, PagoRequest request) {
        if (cuenta.getSaldo().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta no tiene saldo pendiente");
        }
        return PagoFactory.saldo(cuenta, request.metodoPago());
    }

    private Pago crearReembolso(Cuenta cuenta, PagoRequest request) {
        BigDecimal monto = request.monto();
        if (monto == null || monto.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El reembolso necesita un monto mayor a cero");
        }
        if (monto.compareTo(cuenta.getTotal()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El reembolso no puede superar el total de la cuenta");
        }
        return PagoFactory.reembolso(cuenta, request.metodoPago(), monto);
    }
}
