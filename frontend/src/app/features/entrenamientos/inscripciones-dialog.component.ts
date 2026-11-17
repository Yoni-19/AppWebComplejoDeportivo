import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { apiErrorMessage } from '../../core/http/api-error';
import { InscripcionEntrenamiento } from '../../core/models/entrenamiento.model';
import { EntrenamientoService } from './entrenamiento.service';

interface DialogData {
  id: number;
  titulo: string;
}

@Component({
  selector: 'app-inscripciones-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatTableModule, MatProgressSpinnerModule, DatePipe],
  template: `
    <h2 mat-dialog-title>Inscriptos · {{ data.titulo }}</h2>
    <mat-dialog-content>
      @if (loading()) {
        <div class="carga"><mat-spinner diameter="28" strokeWidth="3" /></div>
      } @else if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      } @else if (inscripciones().length === 0) {
        <p class="vacio">Todavia no hay inscriptos.</p>
      } @else {
        <table mat-table [dataSource]="inscripciones()" class="tabla">
          <ng-container matColumnDef="cliente">
            <th mat-header-cell *matHeaderCellDef>Cliente</th>
            <td mat-cell *matCellDef="let i">{{ i.usuarioNombre }}</td>
          </ng-container>
          <ng-container matColumnDef="fecha">
            <th mat-header-cell *matHeaderCellDef>Inscripcion</th>
            <td mat-cell *matCellDef="let i">{{ i.fechaInscripcion | date: 'dd/MM/yy HH:mm' }}</td>
          </ng-container>
          <ng-container matColumnDef="estado">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let i">{{ i.estado }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columnas"></tr>
          <tr mat-row *matRowDef="let row; columns: columnas"></tr>
        </table>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: `
    .carga { display: flex; justify-content: center; padding: 24px 0; }
    .vacio { padding: 16px 0; opacity: 0.7; }
    .error { color: var(--mat-sys-error); }
    .tabla { width: 100%; }
  `,
})
export class InscripcionesDialogComponent {
  private readonly service = inject(EntrenamientoService);
  readonly data = inject<DialogData>(MAT_DIALOG_DATA);

  readonly columnas = ['cliente', 'fecha', 'estado'];
  readonly inscripciones = signal<InscripcionEntrenamiento[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() {
    this.service.listarInscripciones(this.data.id).subscribe({
      next: (inscripciones) => {
        this.inscripciones.set(inscripciones);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(err, 'No se pudieron cargar los inscriptos'));
      },
    });
  }
}
