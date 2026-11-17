import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { Reserva } from '../../core/models/reserva.model';
import { AuthService } from '../../core/services/auth.service';
import { ConfirmDialogComponent } from '../../shared/ui/confirm-dialog.component';
import { PageHeader } from '../../shared/ui/page-header.component';
import { ReservaService } from './reserva.service';

@Component({
  selector: 'app-reserva-list',
  standalone: true,
  imports: [RouterLink, MatTableModule, MatButtonModule, MatIconModule, MatChipsModule, MatProgressSpinnerModule, DatePipe, DecimalPipe, PageHeader],
  templateUrl: './reserva-list.component.html',
  styleUrl: './reserva-list.component.scss',
})
export class ReservaListComponent {
  private readonly service = inject(ReservaService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly auth = inject(AuthService);

  readonly reservas = signal<Reserva[]>([]);
  readonly loading = signal(true);
  /** El rol decide las columnas y los botones; el backend igual valida la propiedad. */
  readonly rol = computed(() => this.auth.sesion()?.rol);
  readonly esCliente = computed(() => this.rol() === 'CLIENTE');
  /** Admin ve historial; utileria y caja solo consultan. Nadie mas opera la fila. */
  readonly puedeGestionar = computed(() => this.auth.esRecepcionista() || this.auth.esCliente());
  readonly displayedColumns = computed(() => {
    const columnas = this.esCliente()
      ? ['id', 'cancha', 'inicio', 'estado', 'monto', 'cuenta']
      : ['id', 'cancha', 'cliente', 'inicio', 'estado', 'monto', 'cuenta'];
    return this.puedeGestionar() ? [...columnas, 'acciones'] : columnas;
  });

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

  /** Solo una reserva confirmada se puede dar de baja; el backend igual lo valida. */
  puedeCancelar(reserva: Reserva): boolean {
    return this.puedeGestionar() && reserva.estado === 'CONFIRMADA';
  }

  /** El check-in y el check-out los registra recepcion, nunca el cliente ni el admin. */
  puedeCheckIn(reserva: Reserva): boolean {
    return this.auth.esRecepcionista() && reserva.estado === 'CONFIRMADA';
  }

  puedeCheckOut(reserva: Reserva): boolean {
    return this.auth.esRecepcionista() && reserva.estado === 'EN_CURSO';
  }

  /** El backend no reprograma reservas canceladas, en curso ni completadas. */
  puedeEditar(reserva: Reserva): boolean {
    return (
      this.puedeGestionar() &&
      reserva.estado !== 'CANCELADA' &&
      reserva.estado !== 'COMPLETADA' &&
      reserva.estado !== 'EN_CURSO'
    );
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
          mensaje:
            'El servicio queda completado. Si la cuenta todavía tiene saldo, caja debe cobrarlo antes: el backend rechaza el cierre.',
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
}
