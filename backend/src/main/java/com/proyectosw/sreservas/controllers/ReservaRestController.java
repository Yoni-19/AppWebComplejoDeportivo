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

import com.proyectosw.sreservas.dto.CargoRequest;
import com.proyectosw.sreservas.dto.CuentaResponse;
import com.proyectosw.sreservas.dto.DevolucionImplementoRequest;
import com.proyectosw.sreservas.dto.EntregaImplementoRequest;
import com.proyectosw.sreservas.dto.ImplementoReservaResponse;
import com.proyectosw.sreservas.dto.ReservaRequest;
import com.proyectosw.sreservas.dto.ReservaResponse;
import com.proyectosw.sreservas.service.ReservaService;
import com.proyectosw.sreservas.service.UtileriaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping ("/api/reservas")
public class ReservaRestController {
    private final ReservaService service;
    private final UtileriaService utileriaService;

    public ReservaRestController(final ReservaService service, final UtileriaService utileriaService) {
        this.service = service;
        this.utileriaService = utileriaService;
    }

    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listarTodos(){
        List<ReservaResponse> reservas = service.listarTodos().stream()
                .map(ReservaResponse::toResponse)
                .toList();
        return ResponseEntity.ok(reservas);
    }

    @GetMapping ("/{id}")
    public ResponseEntity<ReservaResponse> buscarPorId(@PathVariable Integer id){
        return service.buscarPorId(id)
                .map(ReservaResponse::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la reserva con id " + id));
    }

    @GetMapping ("/{id}/cuenta")
    public ResponseEntity<CuentaResponse> cuenta(@PathVariable Integer id){
        return ResponseEntity.ok(CuentaResponse.toResponse(service.buscarCuenta(id)));
    }

    /** Estado de los implementos de la reserva: pedidos, entregados, devueltos, pendientes. */
    @GetMapping ("/{id}/implementos")
    public ResponseEntity<ImplementoReservaResponse> implementos(@PathVariable Integer id){
        return ResponseEntity.ok(utileriaService.listar(id));
    }

    @PostMapping ("/{id}/implementos/entrega")
    public ResponseEntity<ImplementoReservaResponse> entregarImplementos(@PathVariable Integer id,
            @RequestBody List<EntregaImplementoRequest> lineas){
        return ResponseEntity.ok(utileriaService.entregar(id, lineas));
    }

    @PostMapping ("/{id}/implementos/devolucion")
    public ResponseEntity<ReservaResponse> devolverImplementos(@PathVariable Integer id,
            @RequestBody List<DevolucionImplementoRequest> lineas){
        return ResponseEntity.ok(ReservaResponse.toResponse(utileriaService.devolver(id, lineas)));
    }

    /** Cargo manual de utileria (danio, perdida o servicio) sobre la reserva. */
    @PostMapping ("/{id}/cargos")
    public ResponseEntity<ReservaResponse> agregarCargo(@PathVariable Integer id,
            @Valid @RequestBody CargoRequest request){
        return ResponseEntity.ok(ReservaResponse.toResponse(utileriaService.agregarCargo(id, request)));
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest request){
        ReservaResponse nuevo = ReservaResponse.toResponse(service.crear(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @PutMapping ("/{id}")
    public ResponseEntity<ReservaResponse> actualizar(@PathVariable Integer id, @Valid @RequestBody ReservaRequest request){
        return ResponseEntity.ok(ReservaResponse.toResponse(service.actualizar(id, request)));
    }

    @PutMapping ("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelar(@PathVariable Integer id){
        return ResponseEntity.ok(ReservaResponse.toResponse(service.cancelar(id)));
    }

    @PutMapping ("/{id}/check-in")
    public ResponseEntity<ReservaResponse> checkIn(@PathVariable Integer id){
        return ResponseEntity.ok(ReservaResponse.toResponse(service.registrarCheckIn(id)));
    }

    @PutMapping ("/{id}/check-out")
    public ResponseEntity<ReservaResponse> checkOut(@PathVariable Integer id,
            @RequestParam (name = "forzar", defaultValue = "false") boolean forzar){
        return ResponseEntity.ok(ReservaResponse.toResponse(service.registrarCheckOut(id, forzar)));
    }

    @DeleteMapping ("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id){
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
