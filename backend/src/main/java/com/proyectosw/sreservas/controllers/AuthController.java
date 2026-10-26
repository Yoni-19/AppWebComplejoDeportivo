package com.proyectosw.sreservas.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyectosw.sreservas.dto.AuthResponse;
import com.proyectosw.sreservas.dto.LoginRequest;
import com.proyectosw.sreservas.dto.UsuarioResponse;
import com.proyectosw.sreservas.service.AuthService;
import com.proyectosw.sreservas.service.CurrentUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(final AuthService authService, final CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Datos del usuario de la sesion actual: es de donde el frontend saca el rol. */
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me() {
        return ResponseEntity.ok(UsuarioResponse.toResponse(currentUserService.get()));
    }
}
