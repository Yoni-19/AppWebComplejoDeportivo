package com.proyectosw.sreservas.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectosw.sreservas.dto.NotificacionResponse;
import com.proyectosw.sreservas.service.NotificacionService;

@RestController
@RequestMapping ("/api/notificaciones")
public class NotificacionRestController {

    private final NotificacionService service;

    public NotificacionRestController(final NotificacionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar() {
        List<NotificacionResponse> notificaciones = service.listarPropias().stream()
                .map(NotificacionResponse::toResponse)
                .toList();
        return ResponseEntity.ok(notificaciones);
    }

    @GetMapping ("/no-leidas/count")
    public ResponseEntity<Map<String, Long>> contarNoLeidas() {
        return ResponseEntity.ok(Map.of("noLeidas", service.contarNoLeidas()));
    }

    @PutMapping ("/{id}/leer")
    public ResponseEntity<NotificacionResponse> marcarLeida(@PathVariable Integer id) {
        return ResponseEntity.ok(NotificacionResponse.toResponse(service.marcarLeida(id)));
    }

    @PutMapping ("/leer-todas")
    public ResponseEntity<Void> marcarTodasLeidas() {
        service.marcarTodasLeidas();
        return ResponseEntity.noContent().build();
    }
}
