import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { apiErrorMessage } from '../../core/http/api-error';
import { ClienteService } from '../clientes/cliente.service';

/** Both password fields must match; the error is shown on the repeat field. */
function clavesIguales(group: AbstractControl): ValidationErrors | null {
  const clave = group.get('clave')?.value;
  const repetir = group.get('repetirClave')?.value;
  return clave === repetir ? null : { clavesDistintas: true };
}

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

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

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
