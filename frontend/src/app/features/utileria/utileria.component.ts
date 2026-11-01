import { CurrencyPipe, DatePipe, formatCurrency } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { apiErrorMessage } from '../../core/http/api-error';
import {
  CargoAlta,
  DevolucionImplemento,
  EntregaImplemento,
  LineaImplementoReserva,
} from '../../core/models/implemento.model';
import { DetalleReserva, Reserva } from '../../core/models/reserva.model';
import { ConfirmDialogComponent } from '../../shared/ui/confirm-dialog.component';
import { PageHeader } from '../../shared/ui/page-header.component';
import { ImplementoService } from '../implementos/implemento.service';
import { ReservaService } from '../reservas/reserva.service';
import { CargoDialogComponent, CargoDialogData } from './cargo-dialog.component';

const TIPOS_CARGO_APLICADO = ['DANIO', 'PERDIDA', 'SERVICIO'];

/**
 * Utilería: entrega los implementos pedidos, recibe la devolución y carga los
 * daños o pérdidas a la reserva. Después caja cobra el total, así que esta
 * pantalla siempre trabaja sobre un turno ya en curso.
 *
 * La devolución pide por separado las unidades que vuelven en buen estado y las
 * dañadas: así una dañada no se confunde con una que nunca volvió. El cargo
 * manual queda como acción secundaria para las excepciones.
 */
@Component({
  selector: 'app-utileria',
  standalone: true,
  imports: [
    MatCard,
    MatCardContent,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule,
    CurrencyPipe,
    DatePipe,
    PageHeader,
  ],
  templateUrl: './utileria.component.html',
  styleUrl: './utileria.component.scss',
})
export class UtileriaComponent {
  private readonly service = inject(ImplementoService);
  private readonly reservasService = inject(ReservaService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly reservas = signal<Reserva[]>([]);
  readonly seleccionadaId = signal<number | null>(null);
  readonly seleccionada = computed(() => this.reservas().find((r) => r.id === this.seleccionadaId()) ?? null);
  readonly lineas = signal<LineaImplementoReserva[]>([]);
  readonly loading = signal(true);
  readonly trabajando = signal(false);
  readonly displayedColumns = ['nombre', 'pedidos', 'entregados', 'devueltos', 'pendientes', 'entrega', 'devolucion'];

  readonly entregas = signal<Record<number, number>>({});
  /** Unidades que vuelven en buen estado. */
  readonly buenas = signal<Record<number, number>>({});
  /** Unidades que vuelven dañadas: se cobran y también cuentan como devueltas. */
  readonly danadas = signal<Record<number, number>>({});

  constructor() {
    this.cargarReservas();
  }

  /** Cargos que ya tiene la reserva: la pantalla los muestra para no duplicarlos. */
  readonly cargosAplicados = computed<DetalleReserva[]>(() =>
    (this.seleccionada()?.detalle ?? []).filter((linea) => TIPOS_CARGO_APLICADO.includes(linea.tipo)),
  );

  /**
   * Vista previa de la devolución con la misma regla del backend: lo que no
   * vuelve se cobra como pérdida y lo dañado se cobra aparte, al valor de reposición.
   */
  readonly resumen = computed(() => {
    let devueltas = 0;
    let danadas = 0;
    let faltantes = 0;
    let cargos = 0;

    for (const linea of this.lineas()) {
      if (linea.pendientes <= 0) continue;
      const buenas = this.buenasDe(linea);
      const danadasLinea = this.danadas()[linea.implementoId] ?? 0;
      const regresan = buenas + danadasLinea;
      const faltante = Math.max(0, linea.pendientes - regresan);

      devueltas += regresan;
      danadas += danadasLinea;
      faltantes += faltante;
      cargos += (danadasLinea + faltante) * linea.valorReposicion;
    }

    return {
      devueltas,
      danadas,
      faltantes,
      cargos,
      total: (this.seleccionada()?.montoTotal ?? 0) + cargos,
    };
  });

  cargarReservas(): void {
    this.loading.set(true);
    this.reservasService.list().subscribe({
      next: (reservas) => {
        this.reservas.set(reservas.filter((r) => r.estado === 'EN_CURSO'));
        this.loading.set(false);
        const actual = this.seleccionadaId();
        if (actual != null && !this.reservas().some((r) => r.id === actual)) {
          this.seleccionadaId.set(null);
          this.lineas.set([]);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudieron cargar las reservas'), 'Cerrar', { duration: 4000 });
      },
    });
  }

  seleccionar(reservaId: number): void {
    this.seleccionadaId.set(reservaId);
    this.cargarEstado();
  }

  private cargarEstado(): void {
    const reservaId = this.seleccionadaId();
    if (reservaId == null) return;

    this.service.estadoImplementos(reservaId).subscribe({
      next: (estado) => {
        this.lineas.set(estado.lineas);
        // Por defecto se asume que todo lo pendiente vuelve en buen estado; el
        // operador baja las buenas o suma las dañadas según lo que ve.
        const buenas: Record<number, number> = {};
        for (const linea of estado.lineas) {
          buenas[linea.implementoId] = linea.pendientes;
        }
        this.buenas.set(buenas);
        this.danadas.set({});
        this.entregas.set({});
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo cargar el estado de los implementos'), 'Cerrar', { duration: 4000 });
      },
    });
  }

  setCantidad(kind: 'entrega' | 'devolucion' | 'danio', linea: LineaImplementoReserva, valor: string): void {
    const id = linea.implementoId;
    const numero = Math.max(0, Math.floor(Number(valor) || 0));

    if (kind === 'entrega') {
      const maximo = Math.max(0, linea.pedidos - linea.entregados);
      this.entregas.update((map) => ({ ...map, [id]: Math.min(numero, maximo) }));
      return;
    }

    if (kind === 'danio') {
      // Las dañadas salen de las unidades que vuelven: si superan lo pendiente,
      // se recorta la cantidad en buen estado para no inventar devoluciones.
      const danadas = Math.min(numero, linea.pendientes);
      const buenas = Math.min(this.buenasDe(linea), linea.pendientes - danadas);
      this.danadas.update((map) => ({ ...map, [id]: danadas }));
      this.buenas.update((map) => ({ ...map, [id]: buenas }));
      return;
    }

    const danadas = this.danadas()[id] ?? 0;
    const buenas = Math.min(numero, linea.pendientes - danadas);
    this.buenas.update((map) => ({ ...map, [id]: buenas }));
  }

  entregar(): void {
    const reservaId = this.seleccionadaId();
    if (reservaId == null) return;

    const lineas: EntregaImplemento[] = this.lineas()
      .map((linea) => ({ implementoId: linea.implementoId, cantidad: this.entregas()[linea.implementoId] ?? 0 }))
      .filter((linea) => linea.cantidad > 0);

    if (lineas.length === 0) {
      this.snackBar.open('Indicá cuántas unidades entregás.', 'Cerrar', { duration: 3000 });
      return;
    }

    this.trabajando.set(true);
    this.service.entregar(reservaId, lineas).subscribe({
      next: () => {
        this.trabajando.set(false);
        this.snackBar.open('Entrega registrada.', 'Cerrar', { duration: 2500 });
        this.cargarEstado();
      },
      error: (err) => {
        this.trabajando.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudo registrar la entrega'), 'Cerrar', { duration: 5000 });
      },
    });
  }

  devolver(): void {
    const reserva = this.seleccionada();
    if (reserva?.id == null) return;

    const lineas: DevolucionImplemento[] = this.lineas()
      .filter((linea) => linea.pendientes > 0)
      .map((linea) => {
        const danadas = this.danadas()[linea.implementoId] ?? 0;
        return {
          implementoId: linea.implementoId,
          devueltas: this.buenasDe(linea) + danadas,
          danadas,
        };
      });

    if (lineas.length === 0) {
      this.snackBar.open('No hay unidades pendientes de devolver.', 'Cerrar', { duration: 3000 });
      return;
    }

    const resumen = this.resumen();
    if (resumen.cargos <= 0) {
      this.registrarDevolucion(reserva.id, lineas);
      return;
    }

    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          titulo: 'Confirmar devolución con cargos',
          mensaje:
            `Se van a cobrar ${this.formato(resumen.cargos)} por daños y pérdidas. ` +
            `El total de la reserva quedará en ${this.formato(resumen.total)}.`,
          confirmar: 'Registrar devolución',
        },
      })
      .afterClosed()
      .subscribe((confirmado?: boolean) => {
        if (confirmado) this.registrarDevolucion(reserva.id!, lineas);
      });
  }

  private registrarDevolucion(reservaId: number, lineas: DevolucionImplemento[]): void {
    this.trabajando.set(true);
    this.service.devolver(reservaId, lineas).subscribe({
      next: (reserva) => {
        this.trabajando.set(false);
        this.snackBar.open(
          `Devolución registrada. Total actualizado: ${this.formato(reserva.montoTotal)}`,
          'Cerrar',
          { duration: 4000 },
        );
        this.cargarEstado();
        this.cargarReservas();
      },
      error: (err) => {
        this.trabajando.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudo registrar la devolución'), 'Cerrar', { duration: 5000 });
      },
    });
  }

  abrirCargoDialog(): void {
    const reserva = this.seleccionada();
    if (reserva?.id == null) {
      this.snackBar.open('Elegí una reserva en curso.', 'Cerrar', { duration: 3000 });
      return;
    }

    this.dialog
      .open<CargoDialogComponent, CargoDialogData, CargoAlta>(CargoDialogComponent, {
        data: { reserva },
        width: '560px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((cargo) => {
        if (!cargo) return;
        this.trabajando.set(true);
        this.service.agregarCargo(reserva.id!, cargo).subscribe({
          next: (actualizada) => {
            this.trabajando.set(false);
            this.snackBar.open(`Cargo agregado. Total: ${this.formato(actualizada.montoTotal)}`, 'Cerrar', {
              duration: 4000,
            });
            this.cargarReservas();
          },
          error: (err) => {
            this.trabajando.set(false);
            this.snackBar.open(apiErrorMessage(err, 'No se pudo agregar el cargo'), 'Cerrar', { duration: 5000 });
          },
        });
      });
  }

  private buenasDe(linea: LineaImplementoReserva): number {
    return this.buenas()[linea.implementoId] ?? linea.pendientes;
  }

  private formato(monto: number | null): string {
    return formatCurrency(monto ?? 0, 'es-AR', 'ARS', 'symbol-narrow', '1.2-2');
  }
}
