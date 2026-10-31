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

import com.proyectosw.sreservas.dto.ImplementoRequest;
import com.proyectosw.sreservas.dto.ImplementoResponse;
import com.proyectosw.sreservas.service.ImplementoService;

import jakarta.validation.Valid;

/**
 * Catalogo de implementos. Cualquier usuario autenticado puede consultarlo
 * (el cliente elige que pedir al reservar); solo el ADMIN lo administra.
 */
@RestController
@RequestMapping ("/api/implementos")
public class ImplementoRestController {
    private final ImplementoService service;

    public ImplementoRestController(final ImplementoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ImplementoResponse>> listarTodos() {
        List<ImplementoResponse> implementos = service.listarTodos().stream()
                .map(ImplementoResponse::toResponse)
                .toList();
        return ResponseEntity.ok(implementos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImplementoResponse> buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id)
                .map(ImplementoResponse::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el implemento con id " + id));
    }

    @PostMapping
    public ResponseEntity<ImplementoResponse> crear(@Valid @RequestBody ImplementoRequest request) {
        ImplementoResponse nuevo = ImplementoResponse.toResponse(service.crear(request.toEntity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImplementoResponse> actualizar(@PathVariable Integer id,
                                                         @Valid @RequestBody ImplementoRequest request) {
        return ResponseEntity.ok(ImplementoResponse.toResponse(service.actualizar(id, request.toEntity())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
