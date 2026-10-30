import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
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
import { ConfirmDialogComponent } from '../../shared/ui/confirm-dialog.component';
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
  private readonly dialog = inject(MatDialog);
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

  /** Solo una reserva confirmada se puede dar de baja; el backend igual lo valida. */
  puedeCancelar(reserva: Reserva): boolean {
    return reserva.estado === 'CONFIRMADA';
  }

  /** El check-in y el check-out los registra el personal, nunca el cliente. */
  puedeCheckIn(reserva: Reserva): boolean {
    return this.auth.esStaff() && reserva.estado === 'CONFIRMADA';
  }

  puedeCheckOut(reserva: Reserva): boolean {
    return this.auth.esStaff() && reserva.estado === 'EN_CURSO';
  }

  /** El backend no reprograma reservas canceladas, en curso ni completadas. */
  puedeEditar(reserva: Reserva): boolean {
    return reserva.estado !== 'CANCELADA' && reserva.estado !== 'COMPLETADA' && reserva.estado !== 'EN_CURSO';
  }

  /** La ventana de check-in la valida el backend; aca solo se avisa el error. */
  checkIn(reserva: Reserva): void {
    if (reserva.id == null) return;
    this.service.checkIn(reserva.id).subscribe({
      next: () => {
        this.snackBar.open('Check-in registrado.', 'Cerrar', { duration: 2500 });
        this.cargar();
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo registrar el check-in'), 'Cerrar', { duration: 5000 });
      },
    });
  }

  checkOut(reserva: Reserva): void {
    if (reserva.id == null) return;
    const id = reserva.id;
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '420px',
        maxWidth: '95vw',
        data: {
          titulo: 'Registrar check-out',
          mensaje: 'El servicio queda completado. Si falta cobrar el saldo, la cuenta sigue abierta.',
          confirmar: 'Check-out',
          peligro: false,
        },
      })
      .afterClosed()
      .subscribe((confirmado?: boolean) => {
        if (!confirmado) return;
        this.service.checkOut(id).subscribe({
          next: () => {
            this.snackBar.open('Check-out registrado.', 'Cerrar', { duration: 2500 });
            this.cargar();
          },
          error: (err) => {
            this.snackBar.open(apiErrorMessage(err, 'No se pudo registrar el check-out'), 'Cerrar', { duration: 5000 });
          },
        });
      });
  }

  /** Baja de negocio: libera el turno y conserva el historial. */
  cancelar(reserva: Reserva): void {
    if (reserva.id == null) return;
    const id = reserva.id;
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '420px',
        maxWidth: '95vw',
        data: {
          titulo: 'Cancelar reserva',
          mensaje:
            'Se libera el horario para otro cliente y la cuenta queda anulada. La reserva y sus pagos se conservan como historial.',
          confirmar: 'Cancelar reserva',
          peligro: true,
        },
      })
      .afterClosed()
      .subscribe((confirmado?: boolean) => {
        if (!confirmado) return;
        this.service.cancelar(id).subscribe({
          next: () => {
            this.snackBar.open('Reserva cancelada.', 'Cerrar', { duration: 2500 });
            this.cargar();
          },
          error: (err) => {
            this.snackBar.open(apiErrorMessage(err, 'No se pudo cancelar la reserva'), 'Cerrar', { duration: 5000 });
          },
        });
      });
  }

  /** Borrado fisico, solo ADMIN: se lleva pagos, cuenta y detalle. */
  eliminar(reserva: Reserva): void {
    if (reserva.id == null) return;
    const id = reserva.id;
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '420px',
        maxWidth: '95vw',
        data: {
          titulo: 'Eliminar reserva',
          mensaje:
            'Se borran la reserva, su cuenta, sus pagos y su detalle. No se puede deshacer. Para dar de baja un servicio, cancelá la reserva.',
          confirmar: 'Eliminar',
          peligro: true,
        },
      })
      .afterClosed()
      .subscribe((confirmado?: boolean) => {
        if (!confirmado) return;
        this.service.remove(id).subscribe({
          next: () => {
            this.reservas.update((list) => list.filter((r) => r.id !== id));
            this.snackBar.open('Reserva eliminada.', 'Cerrar', { duration: 2500 });
          },
          error: (err) => {
            this.snackBar.open(apiErrorMessage(err, 'No se pudo eliminar la reserva'), 'Cerrar', { duration: 5000 });
          },
        });
      });
  }
}
