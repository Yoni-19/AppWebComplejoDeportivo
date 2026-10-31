import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { apiErrorMessage } from '../../core/http/api-error';
import { DevolucionImplemento, EntregaImplemento, LineaImplementoReserva, TIPOS_CARGO, TipoCargo } from '../../core/models/implemento.model';
import { Reserva } from '../../core/models/reserva.model';
import { PageHeader } from '../../shared/ui/page-header.component';
import { ImplementoService } from '../implementos/implemento.service';
import { ReservaService } from '../reservas/reserva.service';

/**
 * Utilería: entrega los implementos pedidos, recibe la devolución y carga los
 * daños o pérdidas a la reserva. Después caja cobra el total, así que esta
 * pantalla siempre trabaja sobre un turno ya en curso.
 */
@Component({
  selector: 'app-utileria',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCard,
    MatCardContent,
    MatFormFieldModule,
    MatInputModule,
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
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly reservas = signal<Reserva[]>([]);
  readonly seleccionadaId = signal<number | null>(null);
  readonly seleccionada = computed(() => this.reservas().find((r) => r.id === this.seleccionadaId()) ?? null);
  readonly lineas = signal<LineaImplementoReserva[]>([]);
  readonly loading = signal(true);
  readonly trabajando = signal(false);
  readonly displayedColumns = ['nombre', 'pedidos', 'entregados', 'devueltos', 'pendientes', 'entrega', 'devolucion'];
  readonly tiposCargo = TIPOS_CARGO;

  readonly entregas = signal<Record<number, number>>({});
  readonly devueltas = signal<Record<number, number>>({});
  readonly danadas = signal<Record<number, number>>({});

  readonly cargoForm = this.fb.nonNullable.group({
    tipo: ['SERVICIO' as TipoCargo, Validators.required],
    descripcion: ['', Validators.required],
    cantidad: [1, [Validators.required, Validators.min(1)]],
    precioUnitario: [0, [Validators.required, Validators.min(0.01)]],
  });

  constructor() {
    this.cargarReservas();
  }

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
        // Las devueltas arrancan en el pendiente: el operador solo las baja
        // cuando una unidad no volvió (esa diferencia se cobra como pérdida).
        const devueltas: Record<number, number> = {};
        for (const linea of estado.lineas) {
          devueltas[linea.implementoId] = linea.pendientes;
        }
        this.devueltas.set(devueltas);
        this.danadas.set({});
        this.entregas.set({});
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo cargar el estado de los implementos'), 'Cerrar', { duration: 4000 });
      },
    });
  }

  setCantidad(kind: 'entrega' | 'devolucion' | 'danio', implementoId: number, valor: string): void {
    const numero = Math.max(0, Number(valor) || 0);
    if (kind === 'entrega') {
      this.entregas.update((map) => ({ ...map, [implementoId]: numero }));
    } else if (kind === 'devolucion') {
      this.devueltas.update((map) => ({ ...map, [implementoId]: numero }));
    } else {
      this.danadas.update((map) => ({ ...map, [implementoId]: numero }));
    }
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
    const reservaId = this.seleccionadaId();
    if (reservaId == null) return;

    const lineas: DevolucionImplemento[] = this.lineas()
      .filter((linea) => linea.pendientes > 0)
      .map((linea) => ({
        implementoId: linea.implementoId,
        devueltas: this.devueltas()[linea.implementoId] ?? linea.pendientes,
        danadas: this.danadas()[linea.implementoId] ?? 0,
      }));

    if (lineas.length === 0) {
      this.snackBar.open('No hay unidades pendientes de devolver.', 'Cerrar', { duration: 3000 });
      return;
    }

    this.trabajando.set(true);
    this.service.devolver(reservaId, lineas).subscribe({
      next: (reserva) => {
        this.trabajando.set(false);
        this.snackBar.open(
          `Devolución registrada. Total actualizado: ${reserva.montoTotal}`,
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

  agregarCargo(): void {
    const reservaId = this.seleccionadaId();
    if (reservaId == null) {
      this.snackBar.open('Elegí una reserva en curso.', 'Cerrar', { duration: 3000 });
      return;
    }
    if (this.cargoForm.invalid) {
      this.cargoForm.markAllAsTouched();
      return;
    }

    this.trabajando.set(true);
    this.service.agregarCargo(reservaId, this.cargoForm.getRawValue()).subscribe({
      next: (reserva) => {
        this.trabajando.set(false);
        this.snackBar.open(`Cargo agregado. Total: ${reserva.montoTotal}`, 'Cerrar', { duration: 4000 });
        this.cargoForm.reset({ tipo: 'SERVICIO', descripcion: '', cantidad: 1, precioUnitario: 0 });
        this.cargarReservas();
      },
      error: (err) => {
        this.trabajando.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudo agregar el cargo'), 'Cerrar', { duration: 5000 });
      },
    });
  }
}
