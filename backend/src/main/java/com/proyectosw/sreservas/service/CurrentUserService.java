package com.proyectosw.sreservas.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.repository.UsuarioRepository;

/**
 * Resuelve el usuario autenticado de la peticion en curso.
 *
 * El JWT solo transporta el correo: el id y el rol se leen de la base en cada
 * peticion. Por eso un cambio de rol se refleja al instante y los tokens
 * emitidos antes siguen siendo validos.
 */
@Service
public class CurrentUserService {

    private final UsuarioRepository usuarioRepository;

    public CurrentUserService(final UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay una sesión activa");
        }
        return usuarioRepository.findByCorreo(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "La sesión no corresponde a un usuario válido"));
    }

    public Rol rol() {
        Rol rol = get().getRol();
        return rol != null ? rol : Rol.CLIENTE;
    }
}
