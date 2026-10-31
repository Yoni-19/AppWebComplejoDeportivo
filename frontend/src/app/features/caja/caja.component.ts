import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { apiErrorMessage } from '../../core/http/api-error';
import { METODOS_PAGO, MetodoPago } from '../../core/models/pago.model';
import { Reserva } from '../../core/models/reserva.model';
import { PageHeader } from '../../shared/ui/page-header.component';
import { PagoService } from '../pagos/pago.service';
import { ReservaService } from '../reservas/reserva.service';

/**
 * Caja: busca las reservas con saldo abierto y las cobra.
 * El monto siempre lo calcula el backend desde el detalle de la reserva, así los
 * daños y pérdidas cargados por utilería quedan incluidos sin pasos extra.
 */
@Component({
  selector: 'app-caja',
  standalone: true,
  imports: [
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
  templateUrl: './caja.component.html',
  styleUrl: './caja.component.scss',
})
export class CajaComponent {
  private readonly reservasService = inject(ReservaService);
  private readonly pagoService = inject(PagoService);
  private readonly snackBar = inject(MatSnackBar);

  readonly reservas = signal<Reserva[]>([]);
  readonly loading = signal(true);
  readonly cobrando = signal<number | null>(null);
  readonly metodosPago = METODOS_PAGO;
  readonly displayedColumns = ['id', 'cliente', 'cancha', 'estado', 'total', 'saldo', 'cobro'];
  private readonly metodos = signal<Record<number, MetodoPago>>({});

  /** Solo las reservas con cuenta abierta y plata todavía pendiente. */
  readonly pendientes = computed(() =>
    this.reservas().filter((r) => r.estadoCuenta === 'ABIERTA' && (r.saldo ?? 0) > 0),
  );

  readonly totalPendiente = computed(() =>
    this.pendientes().reduce((total, r) => total + (r.saldo ?? 0), 0),
  );

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.reservasService.list().subscribe({
      next: (reservas) => {
        this.reservas.set(reservas);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudieron cargar las reservas'), 'Cerrar', { duration: 4000 });
      },
    });
  }

  metodoDe(reservaId: number | null): MetodoPago {
    if (reservaId == null) return 'EFECTIVO';
    return this.metodos()[reservaId] ?? 'EFECTIVO';
  }

  setMetodo(reservaId: number | null, metodo: MetodoPago): void {
    if (reservaId == null) return;
    this.metodos.update((map) => ({ ...map, [reservaId]: metodo }));
  }

  /** POST /api/pagos con SALDO: el backend cobra el saldo exacto que queda. */
  cobrar(reserva: Reserva): void {
    if (reserva.id == null) return;
    this.cobrando.set(reserva.id);
    this.pagoService.crear({ tipo: 'SALDO', metodoPago: this.metodoDe(reserva.id), reservaId: reserva.id }).subscribe({
      next: (pago) => {
        this.cobrando.set(null);
        this.snackBar.open(`Saldo cobrado: ${pago.monto}`, 'Cerrar', { duration: 3000 });
        this.cargar();
      },
      error: (err) => {
        this.cobrando.set(null);
        this.snackBar.open(apiErrorMessage(err, 'No se pudo cobrar el saldo'), 'Cerrar', { duration: 5000 });
      },
    });
  }
}
