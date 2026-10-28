import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import {
  ClavesIgualesErrorStateMatcher,
  clavesIguales,
} from '../../shared/validators/claves-iguales';
import { ClienteService } from '../clientes/cliente.service';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [ReactiveFormsModule, MatCardModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrl: './registro.component.scss',
})
export class RegistroComponent {
  private readonly fb = inject(FormBuilder);
  private readonly clientes = inject(ClienteService);
  private readonly router = inject(Router);

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
      next: () => {
        this.loading.set(false);
        // El alta siempre nace como CLIENTE: el rol se asigna despues desde el panel de un ADMIN.
        this.router.navigate(['/login'], { queryParams: { registrado: '1' } });
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(apiErrorMessage(err, 'No se pudo crear la cuenta'));
      },
    });
  }
}
