package com.proyectosw.sreservas.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.model.Implemento;
import com.proyectosw.sreservas.repository.DetalleReservaRepository;
import com.proyectosw.sreservas.repository.ImplementoRepository;
import com.proyectosw.sreservas.repository.MovimientoImplementoRepository;

import jakarta.transaction.Transactional;

/**
 * Catalogo de implementos del complejo: pelotas, redes, petos.
 *
 * No administra stock ni inventario (eso vive fuera del sistema). Solo define
 * que se puede ofrecer al reservar y el valor de reposicion que se usa como
 * precio sugerido cuando una unidad se pierde o se dania.
 */
@Service
public class ImplementoService {
    private final ImplementoRepository repository;
    private final MovimientoImplementoRepository movimientoRepository;
    private final DetalleReservaRepository detalleRepository;

    public ImplementoService(final ImplementoRepository repository,
            final MovimientoImplementoRepository movimientoRepository,
            final DetalleReservaRepository detalleRepository) {
        this.repository = repository;
        this.movimientoRepository = movimientoRepository;
        this.detalleRepository = detalleRepository;
    }

    public List<Implemento> listarTodos() {
        return repository.findAll();
    }

    public Optional<Implemento> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public Implemento crear(Implemento datos) {
        validar(datos);
        repository.findByNombreIgnoreCase(datos.getNombre()).ifPresent(existente -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un implemento con el nombre " + datos.getNombre());
        });
        return repository.save(datos);
    }

    @Transactional
    public Implemento actualizar(Integer id, Implemento datos) {
        Implemento existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el implemento con id " + id));

        if (datos.getNombre() != null && !datos.getNombre().isBlank()
                && !datos.getNombre().equalsIgnoreCase(existente.getNombre())) {
            repository.findByNombreIgnoreCase(datos.getNombre()).ifPresent(otro -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un implemento con el nombre " + datos.getNombre());
            });
            existente.setNombre(datos.getNombre());
        }
        if (datos.getValorReposicion() != null) {
            existente.setValorReposicion(datos.getValorReposicion());
        }
        existente.setActivo(datos.isActivo());
        return repository.save(existente);
    }

    /**
     * Borrado del catalogo. Si el implemento ya participo de alguna reserva se
     * rechaza: conviene desactivarlo para no romper el historial. Se mira tanto
     * el movimiento fisico como la linea del pedido, porque un implemento pedido
     * y todavia no entregado ya esta referenciado por la reserva.
     */
    @Transactional
    public void eliminar(Integer id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No existe el implemento con id " + id);
        }
        if (movimientoRepository.existsByImplemento_Id(id)
                || detalleRepository.existsByImplemento_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El implemento tiene reservas o movimientos registrados; desactivalo en vez de borrarlo");
        }
        repository.deleteById(id);
    }

    private void validar(Implemento datos) {
        if (datos.getNombre() == null || datos.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre del implemento es obligatorio");
        }
        if (datos.getValorReposicion() == null) {
            datos.setValorReposicion(BigDecimal.ZERO);
        }
        if (datos.getValorReposicion().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El valor de reposicion no puede ser negativo");
        }
    }
}
