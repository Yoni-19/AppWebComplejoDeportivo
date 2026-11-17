import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface ResolverMantenimientoData {
  canchaNombre: string;
  motivo: string;
}

/** Cierre de un bloqueo: exige registrar la solución antes de liberar la cancha. */
@Component({
  selector: 'app-resolver-mantenimiento-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Resolver mantenimiento</h2>
    <mat-dialog-content>
      <p class="resumen">
        <strong>{{ data.canchaNombre }}</strong> · {{ data.motivo }}
      </p>
      <p class="aviso">
        La cancha vuelve a quedar disponible y recepción recibe el aviso. La solución queda
        registrada en el historial para que el admin pueda consultarla.
      </p>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Solución aplicada</mat-label>
        <textarea
          matInput
          [formControl]="solucion"
          rows="4"
          maxlength="500"
          placeholder="Qué se reparó, revisó o reemplazó"
        ></textarea>
        <mat-hint align="end">{{ solucion.value.length }} / 500</mat-hint>
        @if (solucion.hasError('required')) {
          <mat-error>La solución es obligatoria</mat-error>
        }
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" [mat-dialog-close]="undefined">Volver</button>
      <button
        mat-flat-button
        color="primary"
        type="button"
        [disabled]="solucion.invalid || !solucion.value.trim()"
        [mat-dialog-close]="solucion.value.trim()"
      >
        Resolver
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .resumen {
      margin: 0 0 8px;
    }
    .aviso {
      margin: 0 0 16px;
      font-size: 0.85rem;
      opacity: 0.75;
    }
    .full-width {
      width: 100%;
      min-width: 360px;
      max-width: 100%;
    }
  `,
})
export class ResolverMantenimientoDialogComponent {
  readonly data = inject<ResolverMantenimientoData>(MAT_DIALOG_DATA);
  readonly solucion = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(500)],
  });
}
