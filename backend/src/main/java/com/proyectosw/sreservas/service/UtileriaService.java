package com.proyectosw.sreservas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.proyectosw.sreservas.dto.CargoRequest;
import com.proyectosw.sreservas.dto.DevolucionImplementoRequest;
import com.proyectosw.sreservas.dto.EntregaImplementoRequest;
import com.proyectosw.sreservas.dto.ImplementoReservaResponse;
import com.proyectosw.sreservas.model.Cuenta;
import com.proyectosw.sreservas.model.DetalleReserva;
import com.proyectosw.sreservas.model.Implemento;
import com.proyectosw.sreservas.model.MovimientoImplemento;
import com.proyectosw.sreservas.model.Reserva;
import com.proyectosw.sreservas.model.Usuario;
import com.proyectosw.sreservas.model.enums.EstadoServicio;
import com.proyectosw.sreservas.model.enums.Rol;
import com.proyectosw.sreservas.model.enums.TipoDetalle;
import com.proyectosw.sreservas.model.enums.TipoMovimientoImplemento;
import com.proyectosw.sreservas.repository.DetalleReservaRepository;
import com.proyectosw.sreservas.repository.ImplementoRepository;
import com.proyectosw.sreservas.repository.MovimientoImplementoRepository;
import com.proyectosw.sreservas.repository.ReservaRepository;

import jakarta.transaction.Transactional;

/**
 * Proceso de utileria: entrega los implementos pedidos, recibe la devolucion y
 * carga los danios o perdidas a la reserva.
 *
 * El implemento pedido vive como linea IMPLEMENTO del detalle (precio cero). El
 * movimiento registra el hecho fisico (quien entrego, quien recibio, cuando) y
 * el cargo DANIO/PERDIDA convierte ese hecho en plata sobre la misma cuenta que
 * despues cobra la caja.
 */
@Service
public class UtileriaService {
    private final ReservaRepository reservaRepository;
    private final ImplementoRepository implementoRepository;
    private final MovimientoImplementoRepository movimientoRepository;
    private final DetalleReservaRepository detalleRepository;
    private final CuentaService cuentaService;
    private final CurrentUserService currentUser;
    private final ReservaService reservaService;

    public UtileriaService(final ReservaRepository reservaRepository,
            final ImplementoRepository implementoRepository,
            final MovimientoImplementoRepository movimientoRepository,
            final DetalleReservaRepository detalleRepository,
            final CuentaService cuentaService,
            final CurrentUserService currentUser,
            final ReservaService reservaService) {
        this.reservaRepository = reservaRepository;
        this.implementoRepository = implementoRepository;
        this.movimientoRepository = movimientoRepository;
        this.detalleRepository = detalleRepository;
        this.cuentaService = cuentaService;
        this.currentUser = currentUser;
        this.reservaService = reservaService;
    }

    /**
     * Estado de implementos de la reserva: pedido, entregado, devuelto y pendiente.
     * Pasa por ReservaService para que un CLIENTE solo vea la suya.
     */
    @Transactional
    public ImplementoReservaResponse listar(Integer reservaId) {
        Reserva reserva = reservaService.buscarPorId(reservaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la reserva con id " + reservaId));
        return estadoDeImplementos(reserva);
    }

    /** Registra la entrega al cliente. Solo sobre un turno en curso. */
    @Transactional
    public ImplementoReservaResponse entregar(Integer reservaId, List<EntregaImplementoRequest> lineas) {
        verificarUtileria();

        Reserva reserva = buscarReserva(reservaId);
        if (reserva.getEstado() != EstadoServicio.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se entregan implementos de una reserva en curso");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La entrega debe indicar al menos un implemento");
        }

        Usuario actual = currentUser.get();
        Map<TipoMovimientoImplemento, Map<Integer, Integer>> movimientos = movimientosPorImplemento(reservaId);
        Map<Integer, Integer> pedidos = pedidosPorImplemento(reserva);

        // Agrupa por implemento para que dos lineas del mismo implemento no se pisen.
        Map<Integer, Integer> aEntregar = new LinkedHashMap<>();
        for (EntregaImplementoRequest linea : lineas) {
            if (linea == null || linea.implementoId() == null || linea.cantidad() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada linea de la entrega necesita implemento y cantidad");
            }
            aEntregar.merge(linea.implementoId(), linea.cantidad(), Integer::sum);
        }

        for (Map.Entry<Integer, Integer> entrada : aEntregar.entrySet()) {
            Implemento implemento = buscarImplemento(entrada.getKey());
            int pedido = pedidos.getOrDefault(implemento.getId(), 0);
            int entregado = movimientos
                    .getOrDefault(TipoMovimientoImplemento.ENTREGA, Map.of())
                    .getOrDefault(implemento.getId(), 0);
            int disponible = pedido - entregado;

            if (entrada.getValue() > disponible) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Solo quedan " + disponible + " unidades de " + implemento.getNombre()
                                + " por entregar en esta reserva");
            }
            registrarMovimiento(reserva, implemento, TipoMovimientoImplemento.ENTREGA,
                    entrada.getValue(), actual);
        }

        return estadoDeImplementos(reserva);
    }

    /**
     * Registra la devolucion. Las unidades que no vuelven se cobran como PERDIDA
     * y las danadas como DANIO, con el precio indicado o el del catalogo.
     */
    @Transactional
    public Reserva devolver(Integer reservaId, List<DevolucionImplementoRequest> lineas) {
        verificarUtileria();

        Reserva reserva = buscarReserva(reservaId);
        if (reserva.getEstado() != EstadoServicio.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se reciben implementos de una reserva en curso");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La devolucion debe indicar al menos un implemento");
        }

        Usuario actual = currentUser.get();
        Map<TipoMovimientoImplemento, Map<Integer, Integer>> movimientos = movimientosPorImplemento(reservaId);

        // Se agrupa por implemento igual que la entrega: dos lineas del mismo
        // implemento calcularian el pendiente con los mismos movimientos y
        // cobrarian dos veces lo mismo.
        Map<Integer, DevolucionAgrupada> agrupadas = agruparDevoluciones(lineas);
        for (Map.Entry<Integer, DevolucionAgrupada> entrada : agrupadas.entrySet()) {
            Implemento implemento = buscarImplemento(entrada.getKey());
            DevolucionAgrupada pedido = entrada.getValue();

            int entregado = movimientos
                    .getOrDefault(TipoMovimientoImplemento.ENTREGA, Map.of())
                    .getOrDefault(implemento.getId(), 0);
            int devuelto = movimientos
                    .getOrDefault(TipoMovimientoImplemento.DEVOLUCION, Map.of())
                    .getOrDefault(implemento.getId(), 0);
            int pendiente = entregado - devuelto;

            if (pedido.devueltas > pendiente) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Solo hay " + pendiente + " unidades de " + implemento.getNombre()
                                + " pendientes de devolver");
            }
            if (pedido.danadas > pedido.devueltas) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Las unidades danadas no pueden superar las devueltas");
            }

            if (pedido.devueltas > 0) {
                registrarMovimiento(reserva, implemento, TipoMovimientoImplemento.DEVOLUCION,
                        pedido.devueltas, actual);
            }

            int faltante = pendiente - pedido.devueltas;
            if (pedido.danadas > 0) {
                BigDecimal precio = precioDe(pedido.precioDanio, implemento, "danio");
                agregarLinea(reserva, implemento, TipoDetalle.DANIO,
                        implemento.getNombre() + " danado", pedido.danadas, precio);
            }
            if (faltante > 0) {
                BigDecimal precio = precioDe(pedido.precioPerdida, implemento, "perdida");
                agregarLinea(reserva, implemento, TipoDetalle.PERDIDA,
                        implemento.getNombre() + " perdido", faltante, precio);
            }
        }

        recalcularTotales(reserva);
        return reservaRepository.save(reserva);
    }

    /**
     * Agrupa las lineas de devolucion por implemento sumando cantidades. Los
     * precios indicados se conservan: si una linea trae danio y otra no, vale
     * el que vino en la primera.
     */
    private Map<Integer, DevolucionAgrupada> agruparDevoluciones(List<DevolucionImplementoRequest> lineas) {
        Map<Integer, DevolucionAgrupada> agrupadas = new LinkedHashMap<>();
        for (DevolucionImplementoRequest linea : lineas) {
            if (linea == null || linea.implementoId() == null || linea.devueltas() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada linea de la devolucion necesita implemento y cantidad devuelta");
            }
            DevolucionAgrupada agrupada = agrupadas.computeIfAbsent(
                    linea.implementoId(), id -> new DevolucionAgrupada());
            agrupada.devueltas += linea.devueltas();
            agrupada.danadas += linea.danadasSeguras();
            if (agrupada.precioDanio == null) {
                agrupada.precioDanio = linea.precioDanio();
            }
            if (agrupada.precioPerdida == null) {
                agrupada.precioPerdida = linea.precioPerdida();
            }
        }
        return agrupadas;
    }

    /** Cantidades y precios de una devolucion ya sumados por implemento. */
    private static final class DevolucionAgrupada {
        private int devueltas;
        private int danadas;
        private BigDecimal precioDanio;
        private BigDecimal precioPerdida;
    }

    /** Cargo manual: un danio detectado despues, un servicio extra, etc. */
    @Transactional
    public Reserva agregarCargo(Integer reservaId, CargoRequest request) {
        verificarUtileria();

        Reserva reserva = buscarReserva(reservaId);
        if (request.tipo() != TipoDetalle.DANIO
                && request.tipo() != TipoDetalle.PERDIDA
                && request.tipo() != TipoDetalle.SERVICIO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se pueden cargar lineas DANIO, PERDIDA o SERVICIO");
        }
        if (reserva.getEstado() == EstadoServicio.CANCELADA
                || reserva.getEstado() == EstadoServicio.NO_ASISTIO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se pueden cargar montos a una reserva cancelada o no asistida");
        }
        if (request.precioUnitario() == null || request.precioUnitario().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El precio del cargo debe ser mayor a cero");
        }

        agregarLinea(reserva, null, request.tipo(), request.descripcion().trim(),
                request.cantidad(), request.precioUnitario());
        recalcularTotales(reserva);
        return reservaRepository.save(reserva);
    }

    /**
     * Recalcula el total de la reserva con todas sus lineas y lo empuja a la
     * cuenta, para que caja cobre exactamente lo que corresponde.
     */
    private void recalcularTotales(Reserva reserva) {
        BigDecimal total = detalleRepository.findByReserva_Id(reserva.getId()).stream()
                .map(DetalleReserva::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        reserva.setMontoTotal(total);

        Cuenta cuenta = reserva.getCuenta();
        if (cuenta != null) {
            cuentaService.recalcularTotal(cuenta, total);
        }
    }

    private void agregarLinea(Reserva reserva, Implemento implemento, TipoDetalle tipo,
            String descripcion, int cantidad, BigDecimal precioUnitario) {
        DetalleReserva linea = new DetalleReserva();
        linea.setReserva(reserva);
        linea.setImplemento(implemento);
        linea.setTipo(tipo);
        linea.setDescripcion(descripcion);
        linea.setCantidad(cantidad);
        linea.setPrecioUnitario(precioUnitario);
        linea.setSubtotal(precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
        detalleRepository.save(linea);
        reserva.getDetalles().add(linea);
    }

    private void registrarMovimiento(Reserva reserva, Implemento implemento,
            TipoMovimientoImplemento tipo, int cantidad, Usuario registradoPor) {
        MovimientoImplemento movimiento = new MovimientoImplemento();
        movimiento.setReserva(reserva);
        movimiento.setImplemento(implemento);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setRegistradoPor(registradoPor);
        movimientoRepository.save(movimiento);
    }

    private ImplementoReservaResponse estadoDeImplementos(Reserva reserva) {
        Map<Integer, Integer> pedidos = pedidosPorImplemento(reserva);
        Map<TipoMovimientoImplemento, Map<Integer, Integer>> movimientos =
                movimientosPorImplemento(reserva.getId());

        List<ImplementoReservaResponse.LineaImplemento> lineas = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entrada : pedidos.entrySet()) {
            int implementoId = entrada.getKey();
            int entregados = movimientos
                    .getOrDefault(TipoMovimientoImplemento.ENTREGA, Map.of())
                    .getOrDefault(implementoId, 0);
            int devueltos = movimientos
                    .getOrDefault(TipoMovimientoImplemento.DEVOLUCION, Map.of())
                    .getOrDefault(implementoId, 0);
            Implemento implemento = implementoRepository.findById(implementoId).orElse(null);
            String nombre = implemento != null ? implemento.getNombre() : "Implemento " + implementoId;
            BigDecimal valorReposicion = implemento != null
                    ? implemento.getValorReposicion()
                    : BigDecimal.ZERO;
            lineas.add(new ImplementoReservaResponse.LineaImplemento(
                    implementoId, nombre, entrada.getValue(), entregados, devueltos,
                    entregados - devueltos, valorReposicion));
        }
        return new ImplementoReservaResponse(reserva.getId(), lineas);
    }

    /** Cantidad pedida por implemento, leyendo las lineas IMPLEMENTO del detalle. */
    private Map<Integer, Integer> pedidosPorImplemento(Reserva reserva) {
        Map<Integer, Integer> pedidos = new LinkedHashMap<>();
        for (DetalleReserva linea : detalleRepository.findByReserva_Id(reserva.getId())) {
            if (linea.getTipo() == TipoDetalle.IMPLEMENTO && linea.getImplemento() != null) {
                pedidos.merge(linea.getImplemento().getId(), linea.getCantidad(), Integer::sum);
            }
        }
        return pedidos;
    }

    private Map<TipoMovimientoImplemento, Map<Integer, Integer>> movimientosPorImplemento(Integer reservaId) {
        Map<TipoMovimientoImplemento, Map<Integer, Integer>> resultado = new LinkedHashMap<>();
        for (MovimientoImplemento movimiento : movimientoRepository.findByReserva_IdOrderByIdAsc(reservaId)) {
            resultado
                    .computeIfAbsent(movimiento.getTipo(), tipo -> new LinkedHashMap<>())
                    .merge(movimiento.getImplemento().getId(), movimiento.getCantidad(), Integer::sum);
        }
        return resultado;
    }

    private BigDecimal precioDe(BigDecimal indicado, Implemento implemento, String concepto) {
        BigDecimal precio = indicado != null ? indicado : implemento.getValorReposicion();
        if (precio == null || precio.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Define el monto del " + concepto + " para " + implemento.getNombre());
        }
        return precio;
    }

    private Reserva buscarReserva(Integer reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la reserva con id " + reservaId));
    }

    private Implemento buscarImplemento(Integer implementoId) {
        return implementoRepository.findById(implementoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el implemento con id " + implementoId));
    }

    /** El proceso de utileria lo opera utileria; el admin puede respaldarlo. */
    private void verificarUtileria() {
        Rol rol = currentUser.get().getRol();
        if (rol != Rol.ADMIN && rol != Rol.UTILERIA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo utileria puede registrar la entrega, la devolucion y los cargos");
        }
    }
}
