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

import com.proyectosw.sreservas.dto.EntrenamientoRequest;
import com.proyectosw.sreservas.dto.EntrenamientoResponse;
import com.proyectosw.sreservas.dto.InscripcionEntrenamientoResponse;
import com.proyectosw.sreservas.model.Entrenamiento;
import com.proyectosw.sreservas.service.EntrenamientoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping ("/api/entrenamientos")
public class EntrenamientoRestController {

    private final EntrenamientoService service;

    public EntrenamientoRestController(final EntrenamientoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<EntrenamientoResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping ("/{id}")
    public ResponseEntity<EntrenamientoResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping ("/{id}/inscripciones")
    public ResponseEntity<List<InscripcionEntrenamientoResponse>> listarInscripciones(
            @PathVariable Integer id) {
        return ResponseEntity.ok(service.listarInscripciones(id));
    }

    @PostMapping
    public ResponseEntity<EntrenamientoResponse> crear(@Valid @RequestBody EntrenamientoRequest request) {
        Entrenamiento creado = service.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.buscarPorId(creado.getId()));
    }

    @PutMapping ("/{id}")
    public ResponseEntity<EntrenamientoResponse> actualizar(@PathVariable Integer id,
            @Valid @RequestBody EntrenamientoRequest request) {
        service.actualizar(id, request);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping ("/{id}/cancelar")
    public ResponseEntity<EntrenamientoResponse> cancelar(@PathVariable Integer id) {
        service.cancelar(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    /** El cliente se inscribe a si mismo; el backend ignora cualquier id del cuerpo. */
    @PostMapping ("/{id}/inscripciones")
    public ResponseEntity<InscripcionEntrenamientoResponse> inscribir(@PathVariable Integer id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(InscripcionEntrenamientoResponse.toResponse(service.inscribir(id)));
    }

    @DeleteMapping ("/{id}/inscripciones")
    public ResponseEntity<Void> cancelarInscripcion(@PathVariable Integer id) {
        service.cancelarInscripcion(id);
        return ResponseEntity.noContent().build();
    }
}
