import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { apiErrorMessage } from '../../core/http/api-error';
import {
  ClavesIgualesErrorStateMatcher,
  clavesIguales,
} from '../../shared/validators/claves-iguales';
import { ClienteService } from './cliente.service';

/**
 * Alta rápida para reservas presenciales: el recepcionista registra al cliente
 * sin salir del formulario de reserva y le entrega la clave en mano.
 */
@Component({
  selector: 'app-cliente-rapido-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './cliente-rapido-dialog.component.html',
  styleUrl: './cliente-rapido-dialog.component.scss',
})
export class ClienteRapidoDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly clientes = inject(ClienteService);
  private readonly dialogRef = inject(MatDialogRef<ClienteRapidoDialogComponent>);

  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly matcher = new ClavesIgualesErrorStateMatcher();

  readonly form = this.fb.nonNullable.group(
    {
      nombres: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      apellidos: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      correo: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      clave: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      repetirClave: ['', Validators.required],
    },
    { validators: clavesIguales },
  );

  constructor() {
    this.form.valueChanges.subscribe(() => this.errorMessage.set(null));
  }

  submit(): void {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set(
        this.form.hasError('clavesDistintas')
          ? 'Las contraseñas no coinciden.'
          : 'Revisá los campos marcados antes de continuar.',
      );
      return;
    }

    this.loading.set(true);
    const { nombres, apellidos, correo, clave } = this.form.getRawValue();

    this.clientes.create({ nombres, apellidos, correo, clave }).subscribe({
      next: (cliente) => {
        this.loading.set(false);
        this.dialogRef.close(cliente);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(apiErrorMessage(err, 'No se pudo registrar el cliente'));
      },
    });
  }

  cancelar(): void {
    this.dialogRef.close();
  }
}
