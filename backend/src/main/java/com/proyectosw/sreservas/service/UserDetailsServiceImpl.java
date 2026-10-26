package com.proyectosw.sreservas.service;

import java.util.Objects;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.repository.UsuarioRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(final UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("No existe el usuario con correo " + correo));
        // El rol sale de la base, no de un valor fijo: es lo que después autoriza cada endpoint.
        Rol rol = Objects.requireNonNullElse(usuario.getRol(), Rol.CLIENTE);
        return org.springframework.security.core.userdetails.User
                .withUsername(correo)
                .password(usuario.getClave())
                .roles(rol.name())
                .build();
    }
}