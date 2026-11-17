import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { Entrenamiento } from '../../core/models/entrenamiento.model';
import { AuthService } from '../../core/services/auth.service';
import { ConfirmDialogComponent } from '../../shared/ui/confirm-dialog.component';
import { PageHeader } from '../../shared/ui/page-header.component';
import { EntrenamientoService } from './entrenamiento.service';
import { InscripcionesDialogComponent } from './inscripciones-dialog.component';

@Component({
  selector: 'app-entrenamiento-list',
  standalone: true,
  imports: [
    RouterLink,
    MatTableModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    DatePipe,
    PageHeader,
  ],
  templateUrl: './entrenamiento-list.component.html',
  styleUrl: './entrenamiento-list.component.scss',
})
export class EntrenamientoListComponent {
  private readonly service = inject(EntrenamientoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly entrenamientos = signal<Entrenamiento[]>([]);
  readonly loading = signal(true);

  /** El admin gobierna y no opera: ve el catalogo, no crea ni cancela. */
  readonly puedeGestionar = computed(() => this.auth.esRecepcionista());
  readonly esCliente = computed(() => this.auth.esCliente());
  readonly displayedColumns = computed(() => {
    const columnas = ['titulo', 'horario', 'estado'];
    return this.puedeGestionar() || this.esCliente() ? [...columnas, 'acciones'] : columnas;
  });

  constructor() {
    if (this.auth.isAuthenticated) {
      this.auth.asegurarSesion().subscribe();
    }
    this.cargar();
  }

  cargar(): void {
    this.loading.set(true);
    this.service.list().subscribe({
      next: (entrenamientos) => {
        this.entrenamientos.set(entrenamientos);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(apiErrorMessage(err, 'No se pudieron cargar los entrenamientos'), 'Cerrar', {
          duration: 4000,
        });
      },
    });
  }

  esFuturo(entrenamiento: Entrenamiento): boolean {
    return new Date(entrenamiento.fechaHoraInicio).getTime() > Date.now();
  }

  verDetalle(entrenamiento: Entrenamiento): void {
    if (entrenamiento.id == null) return;
    this.router.navigate(['/entrenamientos', entrenamiento.id]);
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

  inscribirse(entrenamiento: Entrenamiento): void {
    if (entrenamiento.id == null) return;
    this.service.inscribir(entrenamiento.id).subscribe({
      next: () => {
        this.snackBar.open('Inscripcion registrada.', 'Cerrar', { duration: 2500 });
        this.cargar();
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo registrar la inscripcion'), 'Cerrar', {
          duration: 5000,
        });
      },
    });
  }

  cancelarInscripcion(entrenamiento: Entrenamiento): void {
    if (entrenamiento.id == null) return;
    this.service.cancelarInscripcion(entrenamiento.id).subscribe({
      next: () => {
        this.snackBar.open('Inscripcion cancelada.', 'Cerrar', { duration: 2500 });
        this.cargar();
      },
      error: (err) => {
        this.snackBar.open(apiErrorMessage(err, 'No se pudo cancelar la inscripcion'), 'Cerrar', {
          duration: 5000,
        });
      },
    });
  }

  verInscriptos(entrenamiento: Entrenamiento): void {
    if (entrenamiento.id == null) return;
    this.dialog.open(InscripcionesDialogComponent, {
      width: '560px',
      maxWidth: '95vw',
      data: { id: entrenamiento.id, titulo: entrenamiento.titulo },
    });
  }

  cancelar(entrenamiento: Entrenamiento): void {
    if (entrenamiento.id == null) return;
    const id = entrenamiento.id;
    this.dialog
      .open(ConfirmDialogComponent, {
        width: '420px',
        maxWidth: '95vw',
        data: {
          titulo: 'Cancelar entrenamiento',
          mensaje:
            'Se libera el horario y se avisa a los inscriptos. La inscripcion conserva el historial.',
          confirmar: 'Cancelar entrenamiento',
          peligro: true,
        },
      })
      .afterClosed()
      .subscribe((confirmado?: boolean) => {
        if (!confirmado) return;
        this.service.cancelar(id).subscribe({
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
}
