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

import com.proyectosw.sreservas.dto.CanchaRequest;
import com.proyectosw.sreservas.dto.CanchaResponse;
import com.proyectosw.sreservas.service.CanchaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping ("/api/canchas")
public class CanchaRestController {
    private final CanchaService service;

    public CanchaRestController(final CanchaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CanchaResponse>> listarTodos() {
        List<CanchaResponse> canchas = service.listarTodos().stream()
                .map(CanchaResponse::toResponse)
                .toList();
        return ResponseEntity.ok(canchas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CanchaResponse> buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id)
                .map(CanchaResponse::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CanchaResponse> crear(@Valid @RequestBody CanchaRequest request) {
        CanchaResponse nueva = CanchaResponse.toResponse(service.crear(request.toEntity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CanchaResponse> actualizar(@PathVariable Integer id,
                                                     @Valid @RequestBody CanchaRequest request) {
        return ResponseEntity.ok(CanchaResponse.toResponse(service.actualizar(id, request.toEntity())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
