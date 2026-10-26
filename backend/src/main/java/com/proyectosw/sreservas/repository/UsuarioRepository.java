package com.proyectosw.sreservas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.Rol;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer>{
    Optional<Usuario> findByCorreo(String correo);

    long countByRol(Rol rol);
}
