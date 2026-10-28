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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.dto.UsuarioRequest;
import com.proyectosw.sreservas.dto.UsuarioResponse;
import com.proyectosw.sreservas.dto.UsuarioRolRequest;
import com.proyectosw.sreservas.dto.UsuarioUpdateRequest;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping ("/api/usuarios")
public class UsuarioRestController {
    private final UsuarioService service;

    public UsuarioRestController(final UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listarTodos(@RequestParam(required = false) Rol rol) {
        List<UsuarioResponse> usuarios = service.listar(rol).stream()
                .map(UsuarioResponse::toResponse)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Integer id) {
        return service.buscarPorId(id)
                .map(UsuarioResponse::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario con id " + id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse nuevo = UsuarioResponse.toResponse(service.crear(request.toEntity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Integer id,
                                                      @Valid @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(UsuarioResponse.toResponse(service.actualizar(id, request.toEntity())));
    }

    @PutMapping("/{id}/rol")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Integer id,
                                                      @Valid @RequestBody UsuarioRolRequest request) {
        return ResponseEntity.ok(UsuarioResponse.toResponse(service.cambiarRol(id, request.rol())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}
