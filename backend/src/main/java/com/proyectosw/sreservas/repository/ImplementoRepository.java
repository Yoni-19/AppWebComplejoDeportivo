package com.proyectosw.sreservas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.Implemento;

public interface ImplementoRepository extends JpaRepository<Implemento, Integer> {

    Optional<Implemento> findByNombreIgnoreCase(String nombre);
}
