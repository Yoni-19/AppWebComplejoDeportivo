package com.proyectosw.sreservas.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectosw.sreservas.dto.MantenimientoRequest;
import com.proyectosw.sreservas.dto.MantenimientoResolucionRequest;
import com.proyectosw.sreservas.dto.MantenimientoResponse;
import com.proyectosw.sreservas.model.Mantenimiento;
import com.proyectosw.sreservas.service.MantenimientoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping ("/api/mantenimientos")
public class MantenimientoRestController {

    private final MantenimientoService service;

    public MantenimientoRestController(final MantenimientoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MantenimientoResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping ("/{id}")
    public ResponseEntity<MantenimientoResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<MantenimientoResponse> abrir(@Valid @RequestBody MantenimientoRequest request) {
        Mantenimiento creado = service.abrir(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.buscarPorId(creado.getId()));
    }

    @PutMapping ("/{id}/resolver")
    public ResponseEntity<MantenimientoResponse> resolver(@PathVariable Integer id,
            @Valid @RequestBody MantenimientoResolucionRequest request) {
        service.resolver(id, request.solucion());
        return ResponseEntity.ok(service.buscarPorId(id));
    }
}
