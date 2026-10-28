import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface ConfirmDialogData {
  titulo: string;
  mensaje: string;
  confirmar: string;
  peligro?: boolean;
}

/** Confirmacion reutilizable antes de una accion destructiva o irreversible. */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ data.titulo }}</h2>
    <mat-dialog-content>{{ data.mensaje }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="false">Volver</button>
      <button
        mat-flat-button
        type="button"
        [color]="data.peligro ? 'warn' : 'primary'"
        [mat-dialog-close]="true"
      >
        {{ data.confirmar }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDialogComponent {
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
}
