import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { METODOS_PAGO, MetodoPago } from '../../core/models/pago.model';
import { Reserva } from '../../core/models/reserva.model';
import { AuthService } from '../../core/services/auth.service';
import { PageHeader } from '../../shared/ui/page-header.component';
import { PagoService } from '../pagos/pago.service';
import { ReservaService } from './reserva.service';

@Component({
  selector: 'app-reserva-list',
  standalone: true,
  imports: [RouterLink, MatTableModule, MatButtonModule, MatIconModule, MatChipsModule, MatMenuModule, MatProgressSpinnerModule, DatePipe, DecimalPipe, PageHeader],
  templateUrl: './reserva-list.component.html',
  styleUrl: './reserva-list.component.scss',
})
export class ReservaListComponent {
  private readonly service = inject(ReservaService);
  private readonly pagoService = inject(PagoService);
  private readonly snackBar = inject(MatSnackBar);
  readonly auth = inject(AuthService);

  readonly reservas = signal<Reserva[]>([]);
  readonly loading = signal(true);
  readonly metodosPago = METODOS_PAGO;
  /** El rol decide las columnas y los botones; el backend igual valida la propiedad. */
  readonly rol = computed(() => this.auth.sesion()?.rol);
  readonly esCliente = computed(() => this.rol() === 'CLIENTE');
  readonly puedeGestionar = computed(() => !!this.rol());
  readonly displayedColumns = computed(() =>
    this.esCliente()
      ? ['id', 'cancha', 'inicio', 'estado', 'monto', 'cuenta', 'acciones']
      : ['id', 'cancha', 'cliente', 'inicio', 'estado', 'monto', 'cuenta', 'acciones'],
  );

  constructor() {
    // El rol no viaja en el token: hay que pedirlo para saber que columnas mostrar.
    if (this.auth.isAuthenticated) {
      this.auth.asegurarSesion().subscribe();
    }

    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.service.list().subscribe({
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

  /** Cobra el saldo pendiente con el metodo elegido; el backend calcula el monto. */
  cobrarSaldo(reserva: Reserva, metodoPago: MetodoPago): void {
    if (reserva.id == null || !reserva.saldo || reserva.saldo <= 0) return;
    this.pagoService.crear({ tipo: 'SALDO', metodoPago, reservaId: reserva.id }).subscribe({
      next: () => {
        this.snackBar.open('Saldo cobrado.', 'Cerrar', { duration: 2500 });
        this.cargar();
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo registrar el pago'), 'Cerrar', { duration: 5000 });
      },
    });
  }

  eliminar(reserva: Reserva): void {
    if (reserva.id == null) return;
    this.service.remove(reserva.id).subscribe({
      next: () => {
        this.reservas.update((list) => list.filter((r) => r.id !== reserva.id));
        this.snackBar.open('Reserva eliminada.', 'Cerrar', { duration: 2500 });
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo eliminar la reserva'), 'Cerrar', { duration: 5000 });
      },
    });
  }
}
