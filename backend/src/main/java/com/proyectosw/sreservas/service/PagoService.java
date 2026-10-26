package com.proyectosw.sreservas.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.enums.EstadoPago;
import com.proyectosw.sreservas.repository.PagoRepository;
import com.proyectosw.sreservas.repository.ReservaRepository;

import jakarta.transaction.Transactional;

@Service
public class PagoService {
    private final PagoRepository repository;
    private final ReservaRepository reservaRepository;

    public PagoService(final PagoRepository repository, final ReservaRepository reservaRepository) {
        this.repository = repository;
        this.reservaRepository = reservaRepository;
    }

    public List<Pago> listarTodos() { 
        return repository.findAll(); 
    }

    public Optional<Pago> buscarPorId(Integer id) { 
        return repository.findById(id);
     }

    @Transactional 
    public Pago crear(Pago pago) {
        if (pago.getReserva() == null || pago.getReserva().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pago debe referenciar una reserva");
        }

        Reserva reserva = reservaRepository.findById(pago.getReserva().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la reserva con id " + pago.getReserva().getId()));
        pago.setReserva(reserva);

        pago.setMonto(reserva.getMontoTotal());           
        if (pago.getFechaPago() == null) pago.setFechaPago(LocalDateTime.now());
        if (pago.getEstadoPago() == null) pago.setEstadoPago(EstadoPago.PENDIENTE);

        return repository.save(pago);
    }

    @Transactional
    public Pago actualizar(Integer id, Pago datos) {
        Pago existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el pago con id " + id));
        if (datos.getMetodoPago() != null) existente.setMetodoPago(datos.getMetodoPago());
        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Integer id) { 
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No existe el pago con id " + id);
        }
        repository.deleteById(id); 
    }
}
