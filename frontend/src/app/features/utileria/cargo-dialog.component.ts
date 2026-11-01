import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CargoAlta, TIPOS_CARGO, TipoCargo } from '../../core/models/implemento.model';
import { DetalleReserva, Reserva } from '../../core/models/reserva.model';

export interface CargoDialogData {
  reserva: Reserva;
}

const TIPOS_YA_APLICADOS = ['DANIO', 'PERDIDA', 'SERVICIO'];

/**
 * Cargo adicional de una reserva: la excepción, no el flujo normal.
 *
 * La devolución ya cobra los daños y las pérdidas de los implementos. Este
 * diálogo existe para un servicio extra o para un daño detectado después, y
 * muestra los cargos ya aplicados para no cobrar dos veces lo mismo.
 */
@Component({
  selector: 'app-cargo-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    CurrencyPipe,
  ],
  templateUrl: './cargo-dialog.component.html',
  styleUrl: './cargo-dialog.component.scss',
})
export class CargoDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<CargoDialogComponent, CargoAlta>);
  readonly data = inject<CargoDialogData>(MAT_DIALOG_DATA);

  readonly tiposCargo = TIPOS_CARGO;
  readonly cargosAplicados: DetalleReserva[] = this.data.reserva.detalle.filter((linea) =>
    TIPOS_YA_APLICADOS.includes(linea.tipo),
  );
  readonly totalAplicado = this.cargosAplicados.reduce((total, linea) => total + linea.subtotal, 0);

  readonly form = this.fb.nonNullable.group({
    tipo: ['DANIO' as TipoCargo, Validators.required],
    descripcion: ['', Validators.required],
    cantidad: [1, [Validators.required, Validators.min(1)]],
    precioUnitario: [0, [Validators.required, Validators.min(0.01)]],
  });

  readonly nuevoTotal = signal(this.data.reserva.montoTotal ?? 0);

  constructor() {
    this.form.valueChanges.subscribe((valor) => {
      const monto = (valor.cantidad ?? 0) * (valor.precioUnitario ?? 0);
      this.nuevoTotal.set((this.data.reserva.montoTotal ?? 0) + monto);
    });
  }

  confirmar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.getRawValue());
  }

  cancelar(): void {
    this.dialogRef.close();
  }
}
