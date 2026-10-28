package com.proyectosw.sreservas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.Cuenta;

public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {

    Optional<Cuenta> findByReserva_Id(Integer reservaId);
}
