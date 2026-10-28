package com.proyectosw.sreservas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyectosw.sreservas.model.Pago;
import com.proyectosw.sreservas.model.enums.TipoPago;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    List<Pago> findByCuenta_Id(Integer cuentaId);

    boolean existsByCuenta_IdAndTipo(Integer cuentaId, TipoPago tipo);

    void deleteByCuenta_Id(Integer cuentaId);
}
