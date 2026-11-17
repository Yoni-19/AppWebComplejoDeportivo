import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { Entrenamiento, EstadoEntrenamiento } from '../../core/models/entrenamiento.model';
import { AuthService } from '../../core/services/auth.service';
import { ConfirmDialogComponent } from '../../shared/ui/confirm-dialog.component';
import { PageHeader } from '../../shared/ui/page-header.component';
import { EntrenamientoService } from './entrenamiento.service';
import { InscripcionesDialogComponent } from './inscripciones-dialog.component';

@Component({
  selector: 'app-entrenamiento-detail',
  standalone: true,
  imports: [
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    DatePipe,
    DecimalPipe,
    PageHeader,
  ],
  templateUrl: './entrenamiento-detail.component.html',
  styleUrl: './entrenamiento-detail.component.scss',
})
export class EntrenamientoDetailComponent {
  private readonly service = inject(EntrenamientoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly auth = inject(AuthService);

  readonly entrenamiento = signal<Entrenamiento | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  private entrenamientoId = 0;

  readonly etiquetaEstado: Record<EstadoEntrenamiento, string> = {
    PROGRAMADO: 'Programado',
    CANCELADO: 'Cancelado',
  };

  /** El admin gobierna y no opera: ve el detalle, no edita ni cancela. */
  readonly puedeGestionar = computed(() => this.auth.esRecepcionista());
  readonly esCliente = computed(() => this.auth.esCliente());
  readonly porcentajeCupo = computed(() => {
    const entrenamiento = this.entrenamiento();
    if (!entrenamiento || entrenamiento.cupoMaximo <= 0) return 0;
    return Math.min(100, Math.round((entrenamiento.inscriptos / entrenamiento.cupoMaximo) * 100));
  });

  constructor() {
    if (this.auth.isAuthenticated) {
      this.auth.asegurarSesion().subscribe();
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : Number.NaN;
    if (!Number.isFinite(id) || id <= 0) {
      this.loading.set(false);
      this.error.set('No existe el entrenamiento solicitado.');
      return;
    }

    this.entrenamientoId = id;
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.get(this.entrenamientoId).subscribe({
      next: (entrenamiento) => {
        this.loading.set(false);
        if (!entrenamiento) {
          this.error.set('No existe el entrenamiento solicitado.');
          return;
        }
        this.entrenamiento.set(entrenamiento);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(err, 'No se pudo cargar el entrenamiento'));
      },
    });
  }

  esFuturo(entrenamiento: Entrenamiento): boolean {
    return new Date(entrenamiento.fechaHoraInicio).getTime() > Date.now();
  }

  puedeEditar(entrenamiento: Entrenamiento): boolean {
    return (
      this.puedeGestionar() &&
      entrenamiento.estado === 'PROGRAMADO' &&
      this.esFuturo(entrenamiento)
    );
  }

  puedeInscribirse(entrenamiento: Entrenamiento): boolean {
    return (
      this.esCliente() &&
      entrenamiento.estado === 'PROGRAMADO' &&
      !entrenamiento.inscripto &&
      entrenamiento.cupoDisponible > 0 &&
      this.esFuturo(entrenamiento)
    );
  }

  puedeCancelarInscripcion(entrenamiento: Entrenamiento): boolean {
    return this.esCliente() && entrenamiento.inscripto && entrenamiento.estado === 'PROGRAMADO';
  }

  duracion(entrenamiento: Entrenamiento): string {
    const minutos = Math.max(
      0,
      Math.round(
        (new Date(entrenamiento.fechaHoraFin).getTime() -
          new Date(entrenamiento.fechaHoraInicio).getTime()) /
          60000,
      ),
    );
    const horas = Math.floor(minutos / 60);
    const resto = minutos % 60;
    if (horas === 0) return `${resto} min`;
    if (resto === 0) return `${horas} h`;
    return `${horas} h ${resto} min`;
  }

  inscribirse(): void {
    this.service.inscribir(this.entrenamientoId).subscribe({
      next: () => {
        this.snackBar.open('Inscripción registrada.', 'Cerrar', { duration: 2500 });
        this.cargar();
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo registrar la inscripción'), 'Cerrar', {
          duration: 5000,
        });
      },
    });
  }

  cancelarInscripcion(): void {
    this.service.cancelarInscripcion(this.entrenamientoId).subscribe({
      next: () => {
        this.snackBar.open('Inscripción cancelada.', 'Cerrar', { duration: 2500 });
        this.cargar();
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo cancelar la inscripción'), 'Cerrar', {
          duration: 5000,
        });
      },
    });
  }

  verInscriptos(): void {
    const entrenamiento = this.entrenamiento();
    if (!entrenamiento?.id) return;
    this.dialog.open(InscripcionesDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data: { id: entrenamiento.id, titulo: entrenamiento.titulo },
    });
  }

  cancelar(): void {
    const entrenamiento = this.entrenamiento();
    if (!entrenamiento?.id) return;
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '420px',
        maxWidth: '95vw',
        data: {
          titulo: 'Cancelar entrenamiento',
          mensaje:
            'Se libera el horario y se avisa a los inscriptos. La inscripción conserva el historial.',
          confirmar: 'Cancelar entrenamiento',
          peligro: true,
        },
      })
      .afterClosed()
      .subscribe((confirmado?: boolean) => {
        if (!confirmado) return;
        this.service.cancelar(entrenamiento.id!).subscribe({
          next: () => {
            this.snackBar.open('Entrenamiento cancelado.', 'Cerrar', { duration: 2500 });
            this.cargar();
          },
          error: (err) => {
            this.snackBar.open(apiErrorMessage(err, 'No se pudo cancelar el entrenamiento'), 'Cerrar', {
              duration: 5000,
            });
          },
        });
      });
  }

  volver(): void {
    this.router.navigate(['/entrenamientos']);
  }
}
