package com.proyectosw.sreservas.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.model.Cancha;
import com.proyectosw.sreservas.repository.CanchaRepository;
import com.proyectosw.sreservas.repository.OcupacionRepository;

import jakarta.transaction.Transactional;

@Service
public class CanchaService {
    private final CanchaRepository repository;
    private final OcupacionRepository ocupacionRepository;

    public CanchaService(final CanchaRepository repository, final OcupacionRepository ocupacionRepository) {
        this.repository = repository;
        this.ocupacionRepository = ocupacionRepository;
    }

    public List<Cancha> listarTodos(){
        return repository.findAll();
    }

    public Optional<Cancha> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public Cancha crear(Cancha cancha) {
        
        if (cancha.getNombre() == null || cancha.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la cancha es obligatorio");
        }
        if (cancha.getPrecioPorHora() == null || cancha.getPrecioPorHora().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio por hora debe ser mayor a cero");
        }
        return repository.save(cancha);
    }

    @Transactional
    public Cancha actualizar(Integer id, Cancha datos) {
        Cancha existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la cancha con id " + id));
        if (datos.getNombre() != null) existente.setNombre(datos.getNombre());
        if (datos.getDeporte() != null) existente.setDeporte(datos.getDeporte());
        if (datos.getPrecioPorHora() != null) existente.setPrecioPorHora(datos.getPrecioPorHora());
        existente.setActiva(datos.isActiva());
        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Integer id) { 
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No existe la cancha con id " + id);
        }
        if (ocupacionRepository.existsByCancha_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar la cancha porque tiene reservas asociadas");
        }
        repository.deleteById(id); 
    }
    
}
