import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { apiErrorMessage } from '../../core/http/api-error';
import { Notificacion, TipoNotificacion } from '../../core/models/notificacion.model';
import { NotificacionService } from '../../core/services/notificacion.service';
import { PageHeader } from '../../shared/ui/page-header.component';

@Component({
  selector: 'app-notificaciones-list',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, DatePipe, PageHeader],
  templateUrl: './notificaciones-list.component.html',
  styleUrl: './notificaciones-list.component.scss',
})
export class NotificacionesListComponent {
  private readonly service = inject(NotificacionService);
  private readonly snackBar = inject(MatSnackBar);

  readonly notificaciones = signal<Notificacion[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (notificaciones) => {
        this.notificaciones.set(notificaciones);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudieron cargar las notificaciones'), 'Cerrar', {
          duration: 4000,
        });
      },
    });
  }

  icono(tipo: TipoNotificacion): string {
    switch (tipo) {
      case 'ENTRENAMIENTO_CANCELADO':
        return 'event_busy';
      case 'ENTRENAMIENTO_REPROGRAMADO':
        return 'event_repeat';
      case 'MANTENIMIENTO_ABIERTO':
        return 'construction';
      case 'MANTENIMIENTO_RESUELTO':
        return 'check_circle';
    }
  }

  marcarLeida(notificacion: Notificacion): void {
    if (notificacion.leida) return;
    this.service.marcarLeida(notificacion.id).subscribe({
      next: () => {
        this.notificaciones.update((list) =>
          list.map((n) => (n.id === notificacion.id ? { ...n, leida: true } : n)),
        );
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo marcar la notificacion'), 'Cerrar', {
          duration: 4000,
        });
      },
    });
  }

  marcarTodas(): void {
    this.service.marcarTodasLeidas().subscribe({
      next: () => {
        this.notificaciones.update((list) => list.map((n) => ({ ...n, leida: true })));
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudieron marcar las notificaciones'), 'Cerrar', {
          duration: 4000,
        });
      },
    });
  }
}
