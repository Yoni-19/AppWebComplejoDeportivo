package com.proyectosw.sreservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTests {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void tomaElRolDelUsuarioYNoUnRolFijo() {
        Usuario usuario = usuarioConRol(Rol.RECEPCIONISTA);
        when(usuarioRepository.findByCorreo("laura@email.com")).thenReturn(Optional.of(usuario));

        UserDetails details = userDetailsService.loadUserByUsername("laura@email.com");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_RECEPCIONISTA");
    }

    @Test
    void unUsuarioSinRolInformadoEntraComoCliente() {
        Usuario usuario = usuarioConRol(null);
        when(usuarioRepository.findByCorreo("pedro@email.com")).thenReturn(Optional.of(usuario));

        UserDetails details = userDetailsService.loadUserByUsername("pedro@email.com");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CLIENTE");
    }

    private Usuario usuarioConRol(Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setCorreo("laura@email.com");
        usuario.setClave("hash-en-claro-cualquiera");
        usuario.setRol(rol);
        return usuario;
    }
}
