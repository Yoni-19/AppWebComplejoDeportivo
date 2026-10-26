package com.proyectosw.sreservas.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.dto.ReservaRequest;
import com.proyectosw.sreservas.dto.ReservaResponse;
import com.proyectosw.sreservas.service.ReservaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping ("/api/reservas")
public class ReservaRestController {
    private final ReservaService service;

    public ReservaRestController(final ReservaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listarTodos(){
        List<ReservaResponse> reservas = service.listarTodos().stream()
                .map(ReservaResponse::toResponse)
                .toList();
        return ResponseEntity.ok(reservas);
    }

    @GetMapping ("/{id}")
    public ResponseEntity<ReservaResponse> buscarPorId(@PathVariable Integer id){
        return service.buscarPorId(id)
                .map(ReservaResponse::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la reserva con id " + id));
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest request){
        ReservaResponse nuevo = ReservaResponse.toResponse(service.crear(request.toEntity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @PutMapping ("/{id}")
    public ResponseEntity<ReservaResponse> actualizar(@PathVariable Integer id, @Valid @RequestBody ReservaRequest request){
        return ResponseEntity.ok(ReservaResponse.toResponse(service.actualizar(id, request.toEntity())));
    }

    @DeleteMapping ("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id){
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
